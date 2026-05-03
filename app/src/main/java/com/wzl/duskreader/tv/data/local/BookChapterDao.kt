package com.wzl.duskreader.tv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.wzl.duskreader.tv.data.entities.BookChapter

@Dao
interface BookChapterDao {

    @Query("SELECT * FROM book_chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getByBookId(bookId: Long): List<BookChapter>

    @Query("SELECT COUNT(*) FROM book_chapters WHERE bookId = :bookId")
    suspend fun countByBookId(bookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<BookChapter>)

    @Query("DELETE FROM book_chapters WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: Long)

    @Transaction
    suspend fun replaceForBook(bookId: Long, chapters: List<BookChapter>) {
        deleteByBookId(bookId)
        insertAll(chapters)
    }
}
