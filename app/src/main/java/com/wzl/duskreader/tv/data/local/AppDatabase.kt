package com.wzl.duskreader.tv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wzl.duskreader.tv.data.model.Book

/**
 * 暮阅主数据库：单例模式 (Singleton Pattern)
 * 核心容器，负责管理所有实体的表和提供 DAO 访问。
 */
@Database(entities = [Book::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    // 提供给外部调用的书籍操作入口
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库单例，确保在整个应用生命周期中只存在一个数据库连接。
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dusk_reader_database" // 数据库文件名
                )
                // 在开发测试阶段，如果版本变迁可以简单地销毁重建表（生产环境需做 Migration）
                .fallbackToDestructiveMigration() 
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
