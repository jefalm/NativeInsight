package com.example.nativeinsight.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.room.Room
import com.example.nativeinsight.data.AppDatabase
import com.example.nativeinsight.data.Flashcard
import com.example.nativeinsight.logic.FlashcardParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(application: Application) : AndroidViewModel(application) {


    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "native-insight-db"
    )
        .fallbackToDestructiveMigration()
        .build()

    val searchQuery = MutableStateFlow("")
    private val refreshTrigger = MutableStateFlow(0)
    val isFlipped = MutableStateFlow(false)

    // Combined stream
    val flashcardPager: Flow<PagingData<Flashcard>> = combine(searchQuery, refreshTrigger) { query, _ ->
        query
    }.flatMapLatest { query ->
        // Determine bucket before creating Pager
        val targetBucket = if (query.isBlank()) determineTargetBucket() else -1

        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isBlank()) {
                    // Use the pre-calculated bucket to fetch
                    getWeightedPagingSource(targetBucket)
                } else {
                    db.flashcardDao().searchFlashcards(query)
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    // Weighted Logic: 90% (0), 6% (1), 3% (2), 1% (3+)
    private suspend fun determineTargetBucket(): Int {
        return withContext(Dispatchers.IO) {
            val dao = db.flashcardDao()
            // Check availability
            val has0 = dao.getCountForBucket(0) > 0
            val has1 = dao.getCountForBucket(1) > 0
            val has2 = dao.getCountForBucket(2) > 0
            val has3 = dao.getCountForBucketAbove(3) > 0

            // If no cards exist at all
            if (!has0 && !has1 && !has2 && !has3) return@withContext 0

            val roll = Random.nextInt(100) // 0 to 99

            // Selection with Fallback Logic
            // Priority: Target -> Next Higher Bucket -> Reset to 0
            when {
                roll < 70 -> resolveBucket(0, has0, has1, has2, has3)
                roll < 96 -> resolveBucket(1, has1, has2, has3, has0)
                roll < 99 -> resolveBucket(2, has2, has3, has0, has1)
                else      -> resolveBucket(3, has3, has0, has1, has2)
            }
        }
    }

    // State for the card currently being edited
    val cardInEditMode = MutableStateFlow<Flashcard?>(null)

    fun setCardForEdit(card: Flashcard?) {
        cardInEditMode.value = card
    }

    fun updateCardField(updatedCard: Flashcard) {
        cardInEditMode.value = updatedCard
    }

    fun saveEditedCard(onComplete: () -> Unit) {
        val card = cardInEditMode.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Because 'card' has an ID, Room knows exactly which row to update
            db.flashcardDao().update(card)

            withContext(Dispatchers.Main) {
                cardInEditMode.value = null
                onComplete()
            }
        }
    }

    // Helper to find the first available bucket in the preferred order
    private fun resolveBucket(primary: Int, hasPrimary: Boolean, vararg fallbacks: Boolean): Int {
        if (hasPrimary) return primary

        // Map fallbacks back to their bucket IDs for return
        // Logic: if primary is 0, fallbacks are (1,2,3). If primary 3, fallbacks (0,1,2)
        val order = when(primary) {
            0 -> listOf(1, 2, 3)
            1 -> listOf(2, 3, 0)
            2 -> listOf(3, 0, 1)
            else -> listOf(0, 1, 2)
        }

        // Find first available
        for (i in order.indices) {
            if (fallbacks[i]) return order[i]
        }
        return 0 // Should not happen if at least one card exists
    }

    private fun getWeightedPagingSource(bucket: Int) = when (bucket) {
        0 -> db.flashcardDao().getRandomFromBucket(0)
        1 -> db.flashcardDao().getRandomFromOldestHalfBucketOne()
        2 -> db.flashcardDao().getRandomFromBucket(2)
        else -> db.flashcardDao().getRandomFromBucketAbove(3)
    }

    // Update Stats & Refresh
    fun refreshDiscovery(currentCard: Flashcard?) {
        // Clear the active card immediately so the hardware buttons
        // don't try to speak the OLD card while the NEW one is loading.
        activeDiscoveryCard.value = null

        if (currentCard != null) {
            viewModelScope.launch(Dispatchers.IO) {
                db.flashcardDao().markCardReviewed(
                    id = currentCard.id,
                    timestamp = System.currentTimeMillis()
                )
                withContext(Dispatchers.Main) {
                    isFlipped.value = false
                    refreshTrigger.value += 1
                }
            }
        } else {
            isFlipped.value = false
            refreshTrigger.value += 1
        }
    }

    // Analytics Helper
    fun getAnalyticsCounts(onResult: (Map<String, Int>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val counts = mapOf(
                "New" to db.flashcardDao().getCountForBucket(0),
                "Seen Once" to db.flashcardDao().getCountForBucket(1),
                "Seen Twice" to db.flashcardDao().getCountForBucket(2),
                "Mastered" to db.flashcardDao().getCountForBucketAbove(3)
            )
            onResult(counts)
        }
    }


    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun toggleFlip() {
        isFlipped.value = !isFlipped.value
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    val parsedCards = FlashcardParser.parse(stream)
                    db.flashcardDao().insertAll(parsedCards)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // The Analytics State
    val analyticsData = db.flashcardDao().getRevisionDistribution()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCardsCount = analyticsData.map { list -> list.sumOf { it.cardCount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun backupDatabase(destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Force all WAL changes into the main .db file
                // This replaces the need to close the database.
                db.openHelper.writableDatabase.query(
                    SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")
                ).use { it.moveToFirst() }

                val context = getApplication<Application>()
                val dbFile = context.getDatabasePath("native-insight-db")

                if (dbFile.exists()) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        dbFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    // No need to close() or re-init! The UI remains alive.
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreDatabase(sourceUri: Uri, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Close the active Room database connection to release file locks
                db.close()

                val context = getApplication<Application>()
                val dbFile = context.getDatabasePath("native-insight-db")
                val walFile = context.getDatabasePath("native-insight-db-wal")
                val shmFile = context.getDatabasePath("native-insight-db-shm")

                // 2. Delete Write-Ahead Logging files (CRUCIAL: otherwise Room will corrupt the backup)
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                // 3. Copy the selected backup file over the app's current database file
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 4. Return to the Main Thread to trigger the app restart
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError()
                }
            }
        }
    }

    // 1. Keep track of the active card for TTS
    val activeDiscoveryCard = MutableStateFlow<Flashcard?>(null)

    fun updateActiveCard(card: Flashcard?) {
        activeDiscoveryCard.value = card
    }

    // 2. A trigger for the Speech-to-Text launcher
    private val _micTrigger = MutableStateFlow(0)
    val micTrigger: Flow<Int> = _micTrigger

    fun triggerMic() {
        _micTrigger.value += 1
    }

    // Signal for the UI to open the microphone (Event-based to avoid triggering on navigation/recomposition)
    private val _micTriggerSignal = MutableSharedFlow<Unit>(replay = 0)
    val micTriggerSignal = _micTriggerSignal.asSharedFlow()

    fun triggerMicFromHardware() {
        viewModelScope.launch {
            _micTriggerSignal.emit(Unit)
        }
    }

    // State to hold our 4-card Semantic Cluster
    val activeCluster = MutableStateFlow<List<Flashcard>>(emptyList())

    // Step 1 & 2: Generate the Cluster
    fun loadSemanticCluster() {
        // Use Default dispatcher because Levenshtein is a CPU-intensive task
        viewModelScope.launch(Dispatchers.Default) {
            val dao = db.flashcardDao()

            // 1. Fetch Anchor
            val anchor = dao.getRandomAnchorCard()
            if (anchor == null) {
                // No fresh cards left! Handle this UI state (e.g., empty list)
                activeCluster.value = emptyList()
                return@launch
            }

            // 2. Fetch Candidates
            val candidates = dao.getFreshCandidates(anchor.id)

            // 3. Sort by Semantic Similarity (Levenshtein)
            val topSimilarCards = candidates.sortedBy { candidate ->
                // Compare the Portuguese Front text
                com.example.nativeinsight.logic.StringMetrics.levenshtein(
                    anchor.frontPt,
                    candidate.frontPt
                )
            }.take(3) // Grab the top 3 closest matches

            // 4. Combine Anchor + Top 3 and emit to UI
            activeCluster.value = listOf(anchor) + topSimilarCards
        }
    }

    // Step 4: Batch Update the Cluster
    fun submitClusterAndRefresh() {
        val currentCards = activeCluster.value
        if (currentCards.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val idsToUpdate = currentCards.map { it.id }

            // Increment revision and update timestamp for ALL 4 cards
            db.flashcardDao().markClusterReviewed(
                ids = idsToUpdate,
                timestamp = System.currentTimeMillis()
            )

            // Trigger the UI to flip back, then load the next cluster
            withContext(Dispatchers.Main) {
                isFlipped.value = false
                loadSemanticCluster() // Immediately fetch the next batch
            }
        }
    }

    // Tracks which of the 4 cards the user just tapped the Mic for
    val targetClusterCard = MutableStateFlow<Flashcard?>(null)

    fun setTargetClusterCard(card: Flashcard) {
        targetClusterCard.value = card
    }

}
