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
        // [New] Determine bucket before creating Pager
        val targetBucket = if (query.isBlank()) determineTargetBucket() else -1

        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isBlank()) {
                    // [New] Use the pre-calculated bucket to fetch
                    getWeightedPagingSource(targetBucket)
                } else {
                    db.flashcardDao().searchFlashcards(query)
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    // [New] Weighted Logic: 90% (0), 6% (1), 3% (2), 1% (3+)
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
                roll < 90 -> resolveBucket(0, has0, has1, has2, has3)
                roll < 96 -> resolveBucket(1, has1, has2, has3, has0)
                roll < 99 -> resolveBucket(2, has2, has3, has0, has1)
                else      -> resolveBucket(3, has3, has0, has1, has2)
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
        1 -> db.flashcardDao().getRandomFromBucket(1)
        2 -> db.flashcardDao().getRandomFromBucket(2)
        else -> db.flashcardDao().getRandomFromBucketAbove(3)
    }

    // [New] Update Stats & Refresh
    fun refreshDiscovery(currentCard: Flashcard?) {
        if (currentCard != null) {
            viewModelScope.launch(Dispatchers.IO) {
                db.flashcardDao().markCardReviewed(
                    concept = currentCard.concept,
                    frontPt = currentCard.frontPt,
                    timestamp = System.currentTimeMillis()
                )
                // Trigger UI update after DB write
                withContext(Dispatchers.Main) {
                    isFlipped.value = false
                    refreshTrigger.value += 1
                }
            }
        } else {
            // Just refresh if no card was present
            isFlipped.value = false
            refreshTrigger.value += 1
        }
    }

    // [New] Analytics Helper
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
                    //val rawText = stream.reader().readText()
                    val parsedCards = FlashcardParser.parse(stream)
                    db.flashcardDao().upsertAll(parsedCards)
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
                // 1. Force a "Checkpoint" to ensure all data in memory/WAL is written to the main file
                //    This prevents backing up an empty or incomplete database.
                db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))

                // 2. Locate the database file
                db.close()

                val dbName = "native-insight-db"
                val context = getApplication<Application>()
                val dbFile = context.getDatabasePath(dbName)

                // 3. Copy data from the db file to the destination Uri (Google Drive)
                if (dbFile.exists()) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        dbFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    // Optional: Emit a success state or Toast here
                }
                // 3. RE-INIT: Trigger a simple query to re-open the database
                // This ensures the next user action doesn't experience a 'cold start' lag
                db.flashcardDao().getCountForBucket(0)

            } catch (e: Exception) {
                e.printStackTrace()
                // Optional: Emit an error state here
            }
        }
    }
}