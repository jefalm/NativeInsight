package com.example.nativeinsight.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Flashcard::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
}