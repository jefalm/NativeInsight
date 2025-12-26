package com.example.nativeinsight.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FlashcardDao {
    @Upsert
    suspend fun upsertAll(flashcards: List<Flashcard>)

    @Query("SELECT * FROM flashcards ORDER BY RANDOM() LIMIT 1")
    fun getDiscoveryCard(): PagingSource<Int, Flashcard>

    @Query("""
        SELECT * FROM flashcards 
        WHERE concept LIKE '%' || :query || '%' 
           OR front_pt LIKE '%' || :query || '%' 
           OR back_en LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
        ORDER BY length(concept) ASC
    """)
    fun searchFlashcards(query: String): PagingSource<Int, Flashcard>

    // Updated: Uses SQLite's internal rowid to maintain "Newest First" order
    @Query("SELECT * FROM flashcards ORDER BY rowid DESC")
    fun getAllFlashcards(): PagingSource<Int, Flashcard>
}