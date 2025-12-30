package com.example.nativeinsight.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    // This ensures you still can't have duplicate text, keeping your Import logic safe
    indices = [Index(value = ["concept", "front_pt"], unique = true)]
)
data class Flashcard(
    // [NEW] Primary Key is now an ID
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "concept")
    val concept: String,

    @ColumnInfo(name = "front_pt")
    val frontPt: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "idiom_density")
    val idiomDensity: Float,

    @ColumnInfo(name = "back_en")
    val backEn: String,

    @ColumnInfo(name = "literal_logic")
    val literalLogic: String,

    @ColumnInfo(name = "revision_count", defaultValue = "0")
    val revisionCount: Int = 0,

    @ColumnInfo(name = "last_reviewed", defaultValue = "0")
    val lastReviewed: Long = 0
)