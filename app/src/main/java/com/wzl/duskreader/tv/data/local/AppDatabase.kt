package com.wzl.duskreader.tv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookChapter

@Database(entities = [Book::class, BookChapter::class], version = 4, exportSchema = false)
@TypeConverters(BookTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookChapterDao(): BookChapterDao
}

object AppDatabaseMigrations {
    /** 引入章节级架构：新增 book_chapters 表，保留 books 表数据 */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS book_chapters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER NOT NULL,
                    chapterIndex INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    byteOffset INTEGER NOT NULL,
                    byteLength INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_book_chapters_bookId_chapterIndex " +
                    "ON book_chapters (bookId, chapterIndex)",
            )
        }
    }
}
