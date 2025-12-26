package com.example.nativeinsight.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Upsert
    suspend fun upsertAll(flashcards: List<Flashcard>)

    @Query("SELECT * FROM flash_cards")
    fun getAllFlashcards(): Flow<List<Flashcard>>
}