package com.example.nativeinsight.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.nativeinsight.data.AppDatabase
import com.example.nativeinsight.logic.FlashcardParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "native-insight-db"
    ).build()

    // Expose the Flow to UI
    val flashcards = db.flashcardDao().getAllFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Read file from URI
                val context = getApplication<Application>().applicationContext
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val rawText = stream.reader().readText()
                    val parsedCards = FlashcardParser.parse(rawText)
                    db.flashcardDao().upsertAll(parsedCards)
                }
            } catch (e: Exception) {
                e.printStackTrace() // Handle error in production
            }
        }
    }
}