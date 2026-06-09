package com.wzl.duskreader.tv.data.repositories

import android.os.Environment
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookKind
import com.wzl.duskreader.tv.data.entities.CoverSource
import com.wzl.duskreader.tv.data.local.BookDao
import com.wzl.duskreader.tv.data.metadata.BookMetadataResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val metadataResolver: BookMetadataResolver,
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
            val existingBook = bookDao.getBookByPath(file.absolutePath)
            val shouldRefreshCover = existingBook?.needsCoverRefresh() ?: true
            val importedBook = buildImportedBook(file, allowNetworkCover = shouldRefreshCover)
            if (existingBook == null) {
                bookDao.insertBook(importedBook)
                importedCount++
            } else {
                val refreshedBook = existingBook.mergeImportedMetadata(importedBook)
                if (refreshedBook != existingBook) {
                    bookDao.updateBook(refreshedBook)
                }
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

    private fun buildImportedBook(file: File, allowNetworkCover: Boolean = true): Book {
        val metadata = metadataResolver.resolve(file, allowNetworkCover = allowNetworkCover)
        return Book(
            title = metadata.title ?: file.nameWithoutExtension,
            author = metadata.author,
            path = file.absolutePath,
            coverPath = metadata.coverPath,
            description = metadata.description,
            format = file.extension.uppercase(),
            bookKind = metadata.bookKind.takeUnless { it == BookKind.Unknown }?.id ?: BookKind.fromFormat(file.extension).id,
            coverSource = metadata.coverSource?.id ?: if (metadata.coverPath == null) CoverSource.None.id else CoverSource.Unknown.id,
            sourceUrl = metadata.detailPageUrl,
            tags = metadata.tags,
            fileSize = file.length(),
            totalSize = file.length(),
        )
    }

    private fun Book.mergeImportedMetadata(imported: Book): Book {
        val fallbackTitle = File(path).nameWithoutExtension
        val shouldReplaceCover = needsCoverRefresh()
        return copy(
            title = if (title == fallbackTitle && imported.title != fallbackTitle) imported.title else title,
            author = author ?: imported.author,
            coverPath = if (shouldReplaceCover) imported.coverPath else coverPath ?: imported.coverPath,
            description = description ?: imported.description,
            bookKind = if (bookKind == BookKind.Unknown.id) imported.bookKind else bookKind,
            coverSource = when {
                shouldReplaceCover -> imported.coverSource
                coverPath.isNullOrBlank() -> imported.coverSource
                coverSource.isNullOrBlank() || coverSource == CoverSource.None.id || coverSource == CoverSource.Unknown.id -> imported.coverSource
                else -> coverSource
            },
            sourceUrl = when {
                shouldReplaceCover -> imported.sourceUrl
                coverSource == CoverSource.Generated.id -> imported.sourceUrl
                else -> sourceUrl ?: imported.sourceUrl
            },
            tags = if (tags.isEmpty()) imported.tags else tags,
            format = imported.format,
            fileSize = imported.fileSize,
            totalSize = imported.totalSize,
        )
    }

    private fun Book.needsCoverRefresh(): Boolean {
        val pathValue = coverPath
        if (pathValue.isNullOrBlank()) return true
        val coverFile = File(pathValue)
        if (!coverFile.isFile || coverFile.length() <= 0) return true
        return coverSource.isNullOrBlank() ||
            coverSource == CoverSource.Unknown.id ||
            coverSource == CoverSource.OnlineDetail.id
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
