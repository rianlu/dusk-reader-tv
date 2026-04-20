package com.wzl.duskreader.tv.data.repository

import android.os.Environment
import com.wzl.duskreader.tv.data.local.BookDao
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 极简仓库类：移除冗余同步操作，提升启动速度
 */
class BookRepository(private val bookDao: BookDao) {

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("txt", "epub")
    }

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    /**
     * 只有在真正需要扫描时才创建默认书籍
     */
    private suspend fun ensureDefaultBookExists() = withContext(Dispatchers.IO) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val bookDir = File(documentsDir, "暮阅")
        if (!bookDir.exists()) bookDir.mkdirs()

        val defaultFile = File(bookDir, "欢迎使用暮阅.txt")
        if (!defaultFile.exists()) {
            try {
                defaultFile.writeText("欢迎使用暮阅 (Dusk Reader TV)\n\n这是一个为您优化的电视阅读器。")
            } catch (e: Exception) {}
        }

        if (bookDao.getBookByPath(defaultFile.absolutePath) == null) {
            bookDao.insertBook(Book(
                title = "欢迎使用暮阅",
                path = defaultFile.absolutePath,
                format = "TXT",
                totalSize = defaultFile.length()
            ))
        }
    }

    suspend fun scanLocalStorage(): Int = withContext(Dispatchers.IO) {
        ensureDefaultBookExists()
        var importedCount = 0
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val bookDir = File(documentsDir, "暮阅")

        if (!bookDir.exists()) return@withContext 0

        val files = bookDir.listFiles() ?: return@withContext 0
        for (file in files) {
            if (!file.isFile || file.extension.lowercase() !in SUPPORTED_EXTENSIONS) continue
            if (bookDao.getBookByPath(file.absolutePath) == null) {
                bookDao.insertBook(Book(
                    title = file.nameWithoutExtension,
                    path = file.absolutePath,
                    format = file.extension.uppercase(),
                    totalSize = file.length()
                ))
                importedCount++
            }
        }
        return@withContext importedCount
    }

    suspend fun findBookByPath(path: String): Book? = bookDao.getBookByPath(path)
    suspend fun insert(book: Book) = bookDao.insertBook(book)
    suspend fun update(book: Book) = bookDao.updateBook(book)
    suspend fun delete(book: Book) = bookDao.deleteBook(book)
}
