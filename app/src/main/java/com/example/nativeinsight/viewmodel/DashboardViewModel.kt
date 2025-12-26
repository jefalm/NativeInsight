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
    // Combined stream: Reacts to search text changes OR the refresh button
    val flashcardPager: Flow<PagingData<Flashcard>> = combine(searchQuery, refreshTrigger) { query, _ ->
        query
    }.flatMapLatest { query ->
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isBlank()) {
                    // Discovery Mode: Single Random Card
                    db.flashcardDao().getDiscoveryCard()
                } else {
                    // Search Mode: Unlimited results
                    db.flashcardDao().searchFlashcards(query)
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    // Forces the random query to re-run
    fun refreshDiscovery() {
        isFlipped.value = false
        refreshTrigger.value += 1
    }

    fun toggleFlip() {
        isFlipped.value = !isFlipped.value
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    val rawText = stream.reader().readText()
                    val parsedCards = FlashcardParser.parse(rawText)
                    db.flashcardDao().upsertAll(parsedCards)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}