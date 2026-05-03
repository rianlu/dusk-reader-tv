package com.wzl.duskreader.tv.data.local

import androidx.room.*
import com.wzl.duskreader.tv.data.entities.Book
import kotlinx.coroutines.flow.Flow

/**
 * 书籍数据访问对象 (DAO)：定义与 Room 数据库的交互方法
 * 使用 Flow 来提供响应式的数据监听功能，使书架能实时刷新。
 */
@Dao
interface BookDao {
    // 获取书架上的所有书籍，并按最后阅读时间倒序排列 (最新的排前面)
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC")
    fun getAllBooks(): Flow<List<Book>>

    // 获取最近阅读的若干本书（章节级架构：lastReadChapter 或 lastReadPosition 任一非零都算）
    @Query(
        "SELECT * FROM books WHERE lastReadChapter > 0 OR lastReadPosition > 0 " +
            "ORDER BY lastReadTime DESC LIMIT :limit",
    )
    fun getRecentBooks(limit: Int): Flow<List<Book>>

    // 根据主键 ID 查找单本书
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): Book?

    // 检查是否已导入过该路径的文件，防止书架出现重复书籍
    @Query("SELECT * FROM books WHERE path = :path LIMIT 1")
    suspend fun getBookByPath(path: String): Book?

    // 插入新书，如果冲突（尽管已有路径检查）则忽略
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: Book): Long

    // 更新书籍信息（主要用于同步进度和更新阅读时间）
    @Update
    suspend fun updateBook(book: Book)

    // 从书架移除一本书
    @Delete
    suspend fun deleteBook(book: Book)
}
