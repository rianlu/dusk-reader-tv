package com.wzl.duskreader.tv.data.repositories

import android.os.Environment
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.local.BookDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    companion object {
        private const val TAG = "BookRepo"
        private val SUPPORTED_EXTENSIONS = setOf("txt", "epub")
        private const val BOOK_DIR_NAME = "暮阅"
    }

    override fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

    override fun getRecentBooks(limit: Int): Flow<List<Book>> = bookDao.getRecentBooks(limit)

    override suspend fun getBookById(id: Long): Book? = bookDao.getBookById(id)

    override suspend fun findBookByPath(path: String): Book? = bookDao.getBookByPath(path)

    override suspend fun insert(book: Book): Long = bookDao.insertBook(book)

    override suspend fun update(book: Book) = bookDao.updateBook(book)

    override suspend fun delete(book: Book) = bookDao.deleteBook(book)

    override suspend fun scanLocalStorage(): Int = withContext(Dispatchers.IO) {
        ensureDefaultBookExists()
        var importedCount = 0
        val bookDir = resolveBookDir() ?: return@withContext 0
        val files = bookDir.listFiles() ?: return@withContext 0
        for (file in files) {
            if (!file.isFile || file.extension.lowercase() !in SUPPORTED_EXTENSIONS) continue
            if (bookDao.getBookByPath(file.absolutePath) == null) {
                bookDao.insertBook(buildImportedBook(file))
                importedCount++
            }
        }
        android.util.Log.d(TAG, "scan done: dir=$bookDir, files=${files.size}, imported=$importedCount")
        importedCount
    }

    private suspend fun ensureDefaultBookExists() = withContext(Dispatchers.IO) {
        val bookDir = resolveBookDir(createIfMissing = true) ?: return@withContext
        val defaultFile = File(bookDir, "欢迎使用暮阅.txt")
        if (!defaultFile.exists()) {
            runCatching {
                defaultFile.writeText("欢迎使用暮阅 (Dusk Reader TV)\n\n这是一个为您优化的电视阅读器。")
            }
        }
        if (bookDao.getBookByPath(defaultFile.absolutePath) == null) {
            bookDao.insertBook(buildImportedBook(defaultFile).copy(title = "欢迎使用暮阅"))
        }
    }

    private fun buildImportedBook(file: File): Book {
        return Book(
            title = file.nameWithoutExtension,
            path = file.absolutePath,
            format = file.extension.uppercase(),
            fileSize = file.length(),
            totalSize = file.length()
        )
    }

    private fun resolveBookDir(createIfMissing: Boolean = false): File? {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val bookDir = File(documentsDir, BOOK_DIR_NAME)
        if (!bookDir.exists()) {
            if (!createIfMissing) return null
            bookDir.mkdirs()
        }
        return bookDir
    }
}
