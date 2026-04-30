package com.wzl.duskreader.tv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wzl.duskreader.tv.data.entities.Book

@Database(entities = [Book::class], version = 3, exportSchema = false)
@TypeConverters(BookTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
