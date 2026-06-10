package com.wzl.duskreader.tv.network

import android.os.Environment
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookKind
import com.wzl.duskreader.tv.data.entities.CoverSource
import com.wzl.duskreader.tv.data.entities.UploadFilePolicy
import com.wzl.duskreader.tv.data.repositories.BookChapterRepository
import com.wzl.duskreader.tv.data.repositories.BookRepository
import com.wzl.duskreader.tv.util.DebugLogger
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.File
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class TransferServerSnapshot(
    val isAvailable: Boolean,
    val isRunning: Boolean,
    val url: String? = null,
    val message: String,
    val lastUploadMessage: String? = null,
    val lastUploadAtMillis: Long? = null,
)

@Singleton
class FileTransferServer @Inject constructor(
    private val repository: BookRepository,
    private val chapterRepository: BookChapterRepository,
) {
    companion object {
        const val DEFAULT_PORT = 8080
        private const val TAG = "TransferServer"
        private const val BOOK_DIR_NAME = "暮阅"
    }

    private var server: ApplicationEngine? = null
    private var isRunning = false
    @Volatile
    private var lastStartError: String? = null
    @Volatile
    private var lastUploadMessage: String? = null
    @Volatile
    private var lastUploadAtMillis: Long? = null

    private val _snapshot = MutableStateFlow(
        TransferServerSnapshot(
            isAvailable = false,
            isRunning = false,
            message = "正在检查当前网络状态",
        ),
    )
    val snapshot: StateFlow<TransferServerSnapshot> = _snapshot.asStateFlow()

    fun start(port: Int = DEFAULT_PORT): TransferServerSnapshot {
        if (!isRunning) {
            refresh(port)
        }
        return snapshot.value
    }

    @Synchronized
    fun refresh(port: Int = DEFAULT_PORT): TransferServerSnapshot {
        val ip = getLocalIpAddress()
        if (ip.isNullOrBlank()) {
            stop()
            return updateSnapshot(
                TransferServerSnapshot(
                    isAvailable = false,
                    isRunning = false,
                    message = "请确认电视已连接 Wi-Fi 或有线网络后再试",
                    lastUploadMessage = lastUploadMessage,
                    lastUploadAtMillis = lastUploadAtMillis,
                ),
            )
        }

        stop()
        startInternal(port)

        val refreshedSnapshot = if (isRunning) {
            TransferServerSnapshot(
                isAvailable = true,
                isRunning = true,
                url = "http://$ip:$port",
                message = "手机或电脑连接同一局域网后即可打开此地址",
                lastUploadMessage = lastUploadMessage,
                lastUploadAtMillis = lastUploadAtMillis,
            )
        } else {
            TransferServerSnapshot(
                isAvailable = true,
                isRunning = false,
                message = lastStartError ?: "传书服务启动失败，请稍后重试",
                lastUploadMessage = lastUploadMessage,
                lastUploadAtMillis = lastUploadAtMillis,
            )
        }

        return updateSnapshot(refreshedSnapshot)
    }

    @Synchronized
    fun stop() {
        server?.stop(1000, 2000)
        isRunning = false
        server = null
    }

    fun isRunning(): Boolean = isRunning

    fun lastErrorMessage(): String? = lastStartError

    fun getLocalIpAddress(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList().flatMap { nif ->
                nif.inetAddresses.toList()
            }.firstOrNull { addr ->
                !addr.isLoopbackAddress &&
                    !addr.hostAddress.isNullOrBlank() &&
                    !addr.hostAddress!!.contains(":")
            }?.hostAddress
        }.getOrNull()
    }

    private fun startInternal(port: Int) {
        lastStartError = null
        try {
            val repo = repository
            server = embeddedServer(CIO, port = port) {
                routing {
                    get("/") { call.respondText(renderUploadPageHtml(), ContentType.Text.Html) }
                    get("/api/status") { call.respondText(renderStatusJson(snapshot.value), ContentType.Application.Json) }
                    get("/api/books") { handleBooksList(call, repo) }
                    post("/api/rescan") { handleRescan(call, repo) }
                    delete("/api/books/{id}") {
                        handleDeleteBook(
                            call = call,
                            repository = repo,
                            chapterRepository = chapterRepository,
                        )
                    }
                    post("/upload") {
                        handleUpload(
                            call = call,
                            repository = repo,
                            chapterRepository = chapterRepository,
                            onUploadResult = { message, atMillis ->
                                recordUploadResult(message, atMillis)
                            },
                        )
                    }
                }
            }.start(wait = false)
            isRunning = true
            DebugLogger.i(TAG, "Server started on port $port")
        } catch (e: Exception) {
            lastStartError = e.message ?: "未知错误"
            DebugLogger.e(TAG, "Failed to start server", e)
        }
    }

    private fun recordUploadResult(message: String, atMillis: Long) {
        lastUploadMessage = message
        lastUploadAtMillis = atMillis
        _snapshot.value = _snapshot.value.copy(
            lastUploadMessage = message,
            lastUploadAtMillis = atMillis,
        )
    }

    private fun updateSnapshot(snapshot: TransferServerSnapshot): TransferServerSnapshot {
        _snapshot.value = snapshot
        return snapshot
    }
}

private fun renderUploadPageHtml(): String = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>暮阅 · 书库管理</title>
  <style>
    :root {
      color-scheme: light;
      --bg: #eef1f5;
      --card: rgba(255,255,255,0.94);
      --text: #18212a;
      --subtle: #5d6b7a;
      --brand: #2049d8;
      --brand-dark: #1738aa;
      --danger: #c73737;
      --border: rgba(24,33,42,0.10);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      padding: 24px;
      background:
        radial-gradient(circle at top left, rgba(32,73,216,0.14), transparent 32%),
        linear-gradient(180deg, #f7f9fb 0%, var(--bg) 100%);
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    main { width: min(100%, 980px); margin: 0 auto; }
    header {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 18px;
      margin-bottom: 18px;
    }
    h1 { margin: 0 0 8px; font-size: 34px; letter-spacing: -0.02em; }
    p { margin: 0; color: var(--subtle); line-height: 1.6; }
    .status { color: #2740a0; font-weight: 600; white-space: nowrap; }
    .grid { display: grid; grid-template-columns: 1fr; gap: 16px; }
    .card {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 28px;
      padding: 22px;
      box-shadow: 0 18px 46px rgba(20, 28, 40, 0.10);
      backdrop-filter: blur(16px);
    }
    .card h2 { margin: 0 0 14px; font-size: 22px; }
    .upload-row { display: grid; grid-template-columns: 1fr auto; gap: 12px; align-items: center; }
    input[type=file] {
      width: 100%;
      padding: 15px;
      border-radius: 16px;
      border: 1px dashed rgba(24,33,42,0.20);
      background: rgba(255,255,255,0.78);
    }
    button {
      border: none;
      border-radius: 16px;
      padding: 14px 20px;
      background: linear-gradient(135deg, var(--brand), #5b7cff);
      color: white;
      font-size: 15px;
      font-weight: 700;
      cursor: pointer;
    }
    button:disabled { opacity: .45; cursor: not-allowed; }
    button.secondary { background: #e9edf5; color: #263448; }
    button.danger { background: rgba(199,55,55,0.10); color: var(--danger); }
    .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
    .message { min-height: 24px; margin-top: 12px; color: var(--subtle); }
    .books { display: grid; gap: 10px; }
    .book {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 14px;
      align-items: center;
      padding: 15px 16px;
      border: 1px solid var(--border);
      border-radius: 18px;
      background: rgba(255,255,255,0.70);
    }
    .book-title { font-weight: 700; margin-bottom: 6px; }
    .meta { color: var(--subtle); font-size: 13px; line-height: 1.5; }
    .empty { color: var(--subtle); padding: 18px; text-align: center; border: 1px dashed var(--border); border-radius: 18px; }
    @media (max-width: 640px) {
      body { padding: 16px; }
      header { display: block; }
      .status { display: block; margin-top: 10px; white-space: normal; }
      .upload-row, .book { grid-template-columns: 1fr; }
      button { width: 100%; }
    }
  </style>
</head>
<body>
  <main>
    <header>
      <div>
        <h1>暮阅 · 书库管理</h1>
        <p>在同一局域网内上传 TXT / EPUB, 查看或删除电视本地书库。</p>
      </div>
      <div id="status" class="status">正在连接电视...</div>
    </header>

    <section class="grid">
      <div class="card">
        <h2>上传书籍</h2>
        <form id="uploadForm" class="upload-row">
          <input id="fileInput" type="file" name="file" accept=".txt,.epub" required />
          <button id="uploadButton" type="submit">上传到电视</button>
        </form>
        <div id="uploadMessage" class="message">同名文件会覆盖旧版本, 并重置旧章节缓存。</div>
      </div>

      <div class="card">
        <div class="toolbar">
          <h2>电视书库</h2>
          <button id="rescanButton" class="secondary" type="button">重新扫描</button>
        </div>
        <div id="books" class="books"><div class="empty">正在加载书库...</div></div>
      </div>
    </section>
  </main>

  <script>
    const booksEl = document.getElementById('books');
    const statusEl = document.getElementById('status');
    const uploadForm = document.getElementById('uploadForm');
    const fileInput = document.getElementById('fileInput');
    const uploadButton = document.getElementById('uploadButton');
    const uploadMessage = document.getElementById('uploadMessage');
    const rescanButton = document.getElementById('rescanButton');

    function formatSize(bytes) {
      if (!bytes) return '0 B';
      const units = ['B', 'KB', 'MB', 'GB'];
      let value = bytes;
      let index = 0;
      while (value >= 1024 && index < units.length - 1) {
        value /= 1024;
        index++;
      }
      return value.toFixed(index === 0 ? 0 : 1) + ' ' + units[index];
    }

    function formatTime(value) {
      if (!value) return '无阅读记录';
      return new Date(value).toLocaleString();
    }

    function escapeHtml(value) {
      return String(value ?? '').replace(/[&<>"']/g, char => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
      }[char]));
    }

    async function requestJson(url, options) {
      const response = await fetch(url, options);
      const text = await response.text();
      if (!response.ok) throw new Error(text || ('请求失败: ' + response.status));
      return text ? JSON.parse(text) : {};
    }

    async function loadStatus() {
      try {
        const status = await requestJson('/api/status');
        statusEl.textContent = status.running ? '服务已就绪' : '服务未启动';
      } catch (error) {
        statusEl.textContent = '连接失败';
      }
    }

    async function loadBooks() {
      booksEl.innerHTML = '<div class="empty">正在加载书库...</div>';
      try {
        const data = await requestJson('/api/books');
        if (!data.books.length) {
          booksEl.innerHTML = '<div class="empty">书库为空, 请先上传 TXT 或 EPUB。</div>';
          return;
        }
        booksEl.innerHTML = data.books.map(book => [
          '<div class="book">',
          '<div>',
          '<div class="book-title">' + escapeHtml(book.title) + '</div>',
          '<div class="meta">' + escapeHtml(book.author || '未知作者') + ' · ' + escapeHtml(book.format) + ' · ' + formatSize(book.fileSize) + '</div>',
          '<div class="meta">最近阅读: ' + formatTime(book.lastReadTime) + '</div>',
          '</div>',
          '<button class="danger" type="button" data-delete="' + book.id + '">删除</button>',
          '</div>'
        ].join('')).join('');
      } catch (error) {
        booksEl.innerHTML = '<div class="empty">加载失败: ' + escapeHtml(error.message) + '</div>';
      }
    }

    uploadForm.addEventListener('submit', async event => {
      event.preventDefault();
      if (!fileInput.files.length) return;
      uploadButton.disabled = true;
      uploadMessage.textContent = '正在上传...';
      try {
        const body = new FormData(uploadForm);
        const response = await fetch('/upload', { method: 'POST', body });
        const text = await response.text();
        if (!response.ok) throw new Error(text || '上传失败');
        uploadMessage.textContent = text;
        fileInput.value = '';
        await loadBooks();
      } catch (error) {
        uploadMessage.textContent = error.message;
      } finally {
        uploadButton.disabled = false;
      }
    });

    rescanButton.addEventListener('click', async () => {
      rescanButton.disabled = true;
      try {
        const result = await requestJson('/api/rescan', { method: 'POST' });
        uploadMessage.textContent = '扫描完成, 新增 ' + result.imported + ' 本';
        await loadBooks();
      } catch (error) {
        uploadMessage.textContent = error.message;
      } finally {
        rescanButton.disabled = false;
      }
    });

    booksEl.addEventListener('click', async event => {
      const id = event.target.dataset.delete;
      if (!id) return;
      if (!confirm('确定从电视书库删除这本书吗? 本地文件也会被删除。')) return;
      event.target.disabled = true;
      try {
        await requestJson('/api/books/' + id, { method: 'DELETE' });
        await loadBooks();
      } catch (error) {
        alert(error.message);
        event.target.disabled = false;
      }
    });

    loadStatus();
    loadBooks();
  </script>
</body>
</html>
""".trimIndent()


private suspend fun handleBooksList(
    call: ApplicationCall,
    repository: BookRepository,
) {
    val books = repository.getAllBooks().first()
    call.respondText(renderBooksJson(books), ContentType.Application.Json)
}

private suspend fun handleRescan(
    call: ApplicationCall,
    repository: BookRepository,
) {
    val imported = repository.scanLocalStorage()
    call.respondText("""{"success":true,"imported":$imported}""", ContentType.Application.Json)
}

private suspend fun handleDeleteBook(
    call: ApplicationCall,
    repository: BookRepository,
    chapterRepository: BookChapterRepository,
) {
    val bookId = call.parameters["id"]?.toLongOrNull()
    if (bookId == null) {
        call.respondText("书籍 ID 无效", ContentType.Text.Plain, HttpStatusCode.BadRequest)
        return
    }

    val book = repository.getBookById(bookId)
    if (book == null) {
        call.respondText("未找到这本书", ContentType.Text.Plain, HttpStatusCode.NotFound)
        return
    }

    withContext(Dispatchers.IO) {
        chapterRepository.replaceForBook(book.id, emptyList())
        repository.delete(book)
        runCatching {
            val file = File(book.path)
            if (file.isFile) file.delete()
        }
    }
    call.respondText("""{"success":true}""", ContentType.Application.Json)
}

private fun renderStatusJson(snapshot: TransferServerSnapshot): String {
    return buildString {
        append('{')
        append("\"available\":").append(snapshot.isAvailable).append(',')
        append("\"running\":").append(snapshot.isRunning).append(',')
        append("\"url\":").append(snapshot.url.toJsonString()).append(',')
        append("\"message\":").append(snapshot.message.toJsonString()).append(',')
        append("\"lastUploadMessage\":").append(snapshot.lastUploadMessage.toJsonString()).append(',')
        append("\"lastUploadAtMillis\":").append(snapshot.lastUploadAtMillis ?: "null")
        append('}')
    }
}

private fun renderBooksJson(books: List<Book>): String {
    return books.joinToString(
        prefix = "{\"books\":[",
        postfix = "]}",
    ) { book ->
        buildString {
            append('{')
            append("\"id\":").append(book.id).append(',')
            append("\"title\":").append(book.title.toJsonString()).append(',')
            append("\"author\":").append(book.author.toJsonString()).append(',')
            append("\"format\":").append(book.format.toJsonString()).append(',')
            append("\"bookKind\":").append(book.bookKind.toJsonString()).append(',')
            append("\"fileSize\":").append(book.fileSize).append(',')
            append("\"lastReadTime\":").append(book.lastReadTime).append(',')
            append("\"importedAt\":").append(book.importedAt)
            append('}')
        }
    }
}

private fun String?.toJsonString(): String {
    if (this == null) return "null"
    return buildString {
        append('"')
        for (char in this@toJsonString) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
        append('"')
    }
}

private suspend fun handleUpload(
    call: ApplicationCall,
    repository: BookRepository,
    chapterRepository: BookChapterRepository,
    onUploadResult: (message: String, atMillis: Long) -> Unit,
) {
    try {
        val multipart = call.receiveMultipart()
        var uploadedBook: Book? = null
        var uploadError: String? = null
        var uploadedFilename: String? = null

        multipart.forEachPart { part ->
            try {
                val filePart = part as? PartData.FileItem ?: return@forEachPart
                if (uploadedBook != null || uploadError != null) return@forEachPart

                val safeName = UploadFilePolicy.resolveSafeFilename(filePart.originalFileName)
                if (safeName == null) {
                    uploadError = "仅支持 TXT/EPUB 文件，且文件名必须合法。"
                    return@forEachPart
                }

                val documentsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS,
                )
                val bookDir = File(documentsDir, "暮阅").also {
                    if (!it.exists()) it.mkdirs()
                }
                val destFile = File(bookDir, safeName)
                uploadedFilename = safeName

                withContext(Dispatchers.IO) {
                    filePart.streamProvider().use { input ->
                        destFile.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val existing = repository.findBookByPath(destFile.absolutePath)
                    val uploadedFormat = safeName.substringAfterLast(".").uppercase()
                    val uploadedTitle = safeName.substringBeforeLast(".")
                    uploadedBook = if (existing != null) {
                        chapterRepository.replaceForBook(existing.id, emptyList())
                        existing.copy(
                            title = uploadedTitle,
                            author = null,
                            coverPath = null,
                            backdropPath = null,
                            description = null,
                            format = uploadedFormat,
                            bookKind = BookKind.fromFormat(uploadedFormat).id,
                            coverSource = CoverSource.None.id,
                            sourceUrl = null,
                            tags = emptyList(),
                            fileSize = destFile.length(),
                            totalSize = destFile.length(),
                            lastReadChapter = 0,
                            lastReadPosition = 0,
                            lastReadTime = System.currentTimeMillis(),
                        ).also { repository.update(it) }
                    } else {
                        val book = Book(
                            title = uploadedTitle,
                            path = destFile.absolutePath,
                            format = uploadedFormat,
                            bookKind = BookKind.fromFormat(uploadedFormat).id,
                            coverSource = CoverSource.None.id,
                            fileSize = destFile.length(),
                            totalSize = destFile.length(),
                        )
                        val bookId = repository.insert(book)
                        book.copy(id = bookId)
                    }
                }
            } finally {
                part.dispose()
            }
        }

        when {
            uploadError != null -> call.respond(HttpStatusCode.BadRequest, uploadError!!)
            uploadedBook != null -> {
                repository.scanLocalStorage()
                onUploadResult("最近上传：${uploadedFilename ?: uploadedBook!!.title}", System.currentTimeMillis())
                call.respondText("上传成功：${uploadedFilename ?: uploadedBook!!.title}", ContentType.Text.Plain)
            }

            else -> call.respond(HttpStatusCode.BadRequest, "未检测到可上传的文件。")
        }
    } catch (error: Exception) {
        DebugLogger.e("TransferServer", "handleUpload failed", error)
        call.respondText(
            "上传失败：${error.message ?: "未知错误"}",
            ContentType.Text.Plain,
            HttpStatusCode.InternalServerError,
        )
    }
}
