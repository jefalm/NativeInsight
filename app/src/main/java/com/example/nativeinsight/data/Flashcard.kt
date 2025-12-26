package com.example.nativeinsight.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "flashcards",
    primaryKeys = ["concept", "front_pt"] // Composite Key
)
data class Flashcard(
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
    val literalLogic: String
)