package com.example.nativeinsight.data

import androidx.room.Database
import androidx.room.RoomDatabase

// [New] Bump version to 4 to trigger fallbackToDestructiveMigration (as per your config)
@Database(entities = [Flashcard::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
}