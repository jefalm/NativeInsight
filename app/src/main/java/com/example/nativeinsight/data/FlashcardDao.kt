package com.example.nativeinsight.data

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import androidx.room.Update
import androidx.room.Insert
import androidx.room.OnConflictStrategy
@Dao
interface FlashcardDao {
    @Upsert
    suspend fun upsertAll(flashcards: List<Flashcard>)

    // --- [New] Weighted Fetch Queries ---

    // FlashcardDao.kt

    @Query("""
    WITH OldestHalf AS (
        SELECT * FROM flashcards 
        WHERE revision_count = 1 
        ORDER BY last_reviewed ASC 
        LIMIT (SELECT COUNT(*) / 2 FROM flashcards WHERE revision_count = 1)
    )
    SELECT * FROM OldestHalf ORDER BY RANDOM() LIMIT 1
""")
    fun getRandomFromOldestHalfBucketOne(): PagingSource<Int, Flashcard>

    // Get a random card with exact revision count (Buckets 0, 1, 2)
//    @Query("SELECT * FROM flashcards WHERE revision_count = :count ORDER BY RANDOM() LIMIT 1")
//    fun getRandomFromBucket(count: Int): PagingSource<Int, Flashcard>
    @Query("SELECT * FROM flashcards WHERE revision_count = :count ORDER BY CASE WHEN :count = 0 THEN id ELSE RANDOM() END DESC LIMIT 1")
    fun getRandomFromBucket(count: Int): PagingSource<Int, Flashcard>

    // Get a random card with revision count >= min (Bucket 3+)
    @Query("SELECT * FROM flashcards WHERE revision_count >= :minCount ORDER BY RANDOM() LIMIT 1")
    fun getRandomFromBucketAbove(minCount: Int): PagingSource<Int, Flashcard>

    // --- [New] Analytics & Logic Helpers ---

    @Query("SELECT COUNT(*) FROM flashcards WHERE revision_count = :count")
    suspend fun getCountForBucket(count: Int): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE revision_count >= :minCount")
    suspend fun getCountForBucketAbove(minCount: Int): Int

    @Query("UPDATE flashcards SET revision_count = revision_count + 1, last_reviewed = :timestamp WHERE id = :id")
    suspend fun markCardReviewed(id: Int, timestamp: Long)

    // --- Existing Queries ---

    @Query(
        """
        SELECT * FROM flashcards 
        WHERE concept LIKE '%' || :query || '%' 
           OR front_pt LIKE '%' || :query || '%' 
           OR back_en LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
        ORDER BY length(concept) ASC
    """
    )
    fun searchFlashcards(query: String): PagingSource<Int, Flashcard>

    @Query("SELECT * FROM flashcards ORDER BY rowid DESC")
    fun getAllFlashcards(): PagingSource<Int, Flashcard>

    @Query("""
        SELECT revision_count as revision_level, COUNT(*) as card_count 
        FROM flashcards 
        GROUP BY revision_count 
        ORDER BY revision_count ASC
    """)
    fun getRevisionDistribution(): Flow<List<RevisionStat>>
    @Update
    suspend fun update(flashcard: Flashcard)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(flashcards: List<Flashcard>)
}

data class RevisionStat(
    @ColumnInfo(name = "revision_level") val revisionLevel: Int,
    @ColumnInfo(name = "card_count") val cardCount: Int
)