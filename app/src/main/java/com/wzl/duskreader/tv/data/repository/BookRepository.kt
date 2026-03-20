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
 * 书籍仓库类：作为数据层与 UI 层之间的桥梁。
 * 遵循推荐的 MVVM 架构模式，负责数据源的协调处理。
 */
class BookRepository(private val bookDao: BookDao) {

    companion object {
        private const val TAG = "BookRepository"
        private val SUPPORTED_EXTENSIONS = setOf("txt", "epub")
    }

    // 暴露书籍流：书架 UI 会订阅这个流，实现书籍导入后的自动刷新
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    /**
     * 创建默认的欢迎书籍（使用说明），确保用户第一次打开时有内容展示。
     */
    suspend fun createDefaultBook() = withContext(Dispatchers.IO) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val bookDir = File(documentsDir, "暮阅")
        
        DebugLogger.i(TAG, "检测默认书籍。目录: ${bookDir.absolutePath}, 存在: ${bookDir.exists()}")
        
        if (!bookDir.exists()) {
            val created = bookDir.mkdirs()
            DebugLogger.i(TAG, "创建目录: $created")
        }

        val defaultFile = File(bookDir, "欢迎使用暮阅.txt")
        if (!defaultFile.exists()) {
            // ... (welcomeContent string unchanged)
            val welcomeContent = """
                # 欢迎使用暮阅 (Dusk Reader TV)
                
                这是一款专为 Android TV 设计的小说阅读器。
                
                ### 快速开始：
                1. **手机传书**：点击左侧导航栏的“传书”按钮，使用手机扫描二维码，即可无线上传小说。
                2. **本地目录**：本应用会自动扫描电视存储中的 `/Documents/暮阅/` 文件夹。
                3. **遥控器操作**：
                   - 【方向键】：翻页/移动焦点
                   - 【确定键】：呼出菜单/确认
                   - 【返回键】：退出阅读/返回上一级
                
                ### 支持格式：
                - 目前支持 TXT 和 EPUB 格式。
                - 后续将支持 WebDAV 资源挂载。
                
                祝您阅读愉快！
            """.trimIndent()
            
            try {
                defaultFile.writeText(welcomeContent)
                DebugLogger.i(TAG, "创建默认欢迎书籍文件成功")
            } catch (e: Exception) {
                DebugLogger.e(TAG, "创建默认欢迎书籍文件失败", e)
            }
        }

        // 核心修正：即便文件已存在，也要确保它在数据库里
        if (defaultFile.exists()) {
            val existing = bookDao.getBookByPath(defaultFile.absolutePath)
            if (existing == null) {
                bookDao.insertBook(Book(
                    title = "欢迎使用暮阅",
                    path = defaultFile.absolutePath,
                    format = "TXT",
                    totalSize = defaultFile.length()
                ))
                DebugLogger.i(TAG, "欢迎书籍已补录入库")
            } else {
                DebugLogger.d(TAG, "欢迎书籍已在库中。ID: ${existing.id}")
            }
        }
    }

    /**
     * 扫描本地存储中的书籍文件，自动导入到数据库。
     */
    suspend fun scanLocalStorage(): Int = withContext(Dispatchers.IO) {
        // 先尝试创建/入库默认书籍
        createDefaultBook()
        
        var importedCount = 0

        // 扫描公共文档目录下的 "暮阅" 文件夹
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val bookDir = File(documentsDir, "暮阅")

        if (!bookDir.exists() || !bookDir.isDirectory) {
            DebugLogger.d(TAG, "书籍目录不存在或不是目录: ${bookDir.absolutePath}")
            return@withContext 0
        }

        val files = bookDir.listFiles() ?: run {
            DebugLogger.e(TAG, "listFiles() 返回 null。可能是权限不足。")
            return@withContext 0
        }
        
        DebugLogger.i(TAG, "扫描目录: ${bookDir.absolutePath}，找到 ${files.size} 个文件")

        for (file in files) {
            if (!file.isFile) {
                DebugLogger.d(TAG, "跳过非文件: ${file.name}")
                continue
            }

            val extension = file.extension.lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                DebugLogger.d(TAG, "跳过不支持格式: ${file.name}")
                continue
            }

            // 检查是否已存在，避免重复导入
            val existing = bookDao.getBookByPath(file.absolutePath)
            if (existing != null) {
                DebugLogger.d(TAG, "书籍已存在，跳过: ${file.name}")
                continue
            }

            val book = Book(
                title = file.nameWithoutExtension,
                path = file.absolutePath,
                format = extension.uppercase(),
                totalSize = file.length()
            )
            bookDao.insertBook(book)
            importedCount++
            DebugLogger.i(TAG, "新导入书籍: ${book.title}")
        }

        DebugLogger.i(TAG, "扫描结束。新导入: $importedCount")
        return@withContext importedCount
    }

    /**
     * 根据数据库中的路径查询单本书，用于查重逻辑。
     */
    suspend fun findBookByPath(path: String): Book? {
        return bookDao.getBookByPath(path)
    }

    /**
     * 将一本新书持久化到数据库。
     */
    suspend fun insert(book: Book) {
        bookDao.insertBook(book)
    }

    /**
     * 更新阅读进度或书籍元数据。
     */
    suspend fun update(book: Book) {
        bookDao.updateBook(book)
    }

    /**
     * 从数据库物理删除书籍（不会删除文件本身）。
     */
    suspend fun delete(book: Book) {
        bookDao.deleteBook(book)
    }
}
