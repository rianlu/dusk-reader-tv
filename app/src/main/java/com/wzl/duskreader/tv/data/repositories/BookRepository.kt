package com.wzl.duskreader.tv.data.repositories

import com.wzl.duskreader.tv.data.entities.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getRecentBooks(limit: Int = 10): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    suspend fun findBookByPath(path: String): Book?
    suspend fun insert(book: Book): Long
    suspend fun update(book: Book)
    suspend fun delete(book: Book)
    suspend fun scanLocalStorage(): Int
}
