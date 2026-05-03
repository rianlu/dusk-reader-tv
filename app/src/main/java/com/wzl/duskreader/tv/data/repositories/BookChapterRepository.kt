package com.wzl.duskreader.tv.data.repositories

import com.wzl.duskreader.tv.data.entities.BookChapter
import com.wzl.duskreader.tv.data.local.BookChapterDao
import javax.inject.Inject
import javax.inject.Singleton

interface BookChapterRepository {
    suspend fun getByBookId(bookId: Long): List<BookChapter>
    suspend fun countByBookId(bookId: Long): Int
    suspend fun replaceForBook(bookId: Long, chapters: List<BookChapter>)
}

@Singleton
class BookChapterRepositoryImpl @Inject constructor(
    private val dao: BookChapterDao,
) : BookChapterRepository {

    override suspend fun getByBookId(bookId: Long): List<BookChapter> = dao.getByBookId(bookId)

    override suspend fun countByBookId(bookId: Long): Int = dao.countByBookId(bookId)

    override suspend fun replaceForBook(bookId: Long, chapters: List<BookChapter>) {
        dao.replaceForBook(bookId, chapters)
    }
}
