package com.wzl.duskreader.tv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wzl.duskreader.tv.data.model.Book

@Database(entities = [Book::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 极速获取单例，不执行任何耗时操作
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dusk_reader_database"
                )
                .fallbackToDestructiveMigration()
                // 关键优化：禁用 Room 的一些昂贵检查
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) 
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
