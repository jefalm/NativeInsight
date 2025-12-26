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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "native-insight-db"
    )
        .fallbackToDestructiveMigration() // CRITICAL: This allows the app to reset the DB since we changed the schema
        .build()

    // 1. The Search Trigger
    val searchQuery = MutableStateFlow("")

    // 2. The Stream (Pager)
    // Whenever 'searchQuery' changes, this restarts the Paging stream with the new filter
    val flashcardPager: Flow<PagingData<Flashcard>> = searchQuery.flatMapLatest { query ->
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isBlank()) db.flashcardDao().getAllFlashcards()
                else db.flashcardDao().searchFlashcards("$query*") // The '*' allows partial matching
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
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