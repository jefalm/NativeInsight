package com.example.nativeinsight.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

// FTS4 creates a specialized SQLite table for massive text datasets
@Fts4
@Entity(tableName = "flash_cards_fts")
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    val id: Int = 0, // FTS requires an Int ID

    @ColumnInfo(name = "concept")
    val concept: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "idiom_density")
    val idiomDensity: Float,

    @ColumnInfo(name = "front_pt")
    val frontPt: String,

    @ColumnInfo(name = "back_en")
    val backEn: String,

    @ColumnInfo(name = "literal_logic")
    val literal: String
)