package com.example.nativeinsight.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flash_cards")
data class Flashcard(
    @PrimaryKey
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