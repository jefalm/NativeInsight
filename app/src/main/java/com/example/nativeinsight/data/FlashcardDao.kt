package com.example.nativeinsight.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FlashcardDao {
    @Upsert
    suspend fun upsertAll(flashcards: List<Flashcard>)

    // By selecting `rowid` and the other columns explicitly, we ensure Room can map them
    // correctly to the Flashcard data class fields, resolving the KSP error.
    @Query("""
    SELECT rowid, concept, category, idiom_density, front_pt, back_en, literal_logic
    FROM flash_cards_fts 
    WHERE flash_cards_fts MATCH :query 
    ORDER BY length(concept) ASC, rowid ASC
""")
    fun searchFlashcards(query: String): PagingSource<Int, Flashcard>

    @Query("SELECT rowid, concept, category, idiom_density, front_pt, back_en, literal_logic FROM flash_cards_fts ORDER BY rowid DESC")
    fun getAllFlashcards(): PagingSource<Int, Flashcard>
}
