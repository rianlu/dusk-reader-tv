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
                message = lastStartError ?: "书库管理服务启动失败，请稍后重试",
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
      color-scheme: dark;
      --bg: #070d15;
      --panel: rgba(13, 22, 34, 0.82);
      --panel-strong: rgba(16, 28, 43, 0.96);
      --panel-soft: rgba(255,255,255,0.06);
      --text: #f7fbff;
      --muted: rgba(247,251,255,0.62);
      --faint: rgba(247,251,255,0.38);
      --brand: #89b4ff;
      --brand-strong: #4f7dff;
      --accent: #ffd58a;
      --danger: #ff7d7d;
      --border: rgba(255,255,255,0.12);
      --shadow: 0 24px 80px rgba(0,0,0,0.38);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      padding: 32px;
      background:
        radial-gradient(circle at 12% 0%, rgba(79,125,255,0.26), transparent 34%),
        radial-gradient(circle at 88% 8%, rgba(255,213,138,0.14), transparent 30%),
        linear-gradient(180deg, #0b1420 0%, var(--bg) 100%);
      color: var(--text);
      font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    main { width: min(100%, 1180px); margin: 0 auto; }
    header {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 24px;
      align-items: end;
      margin-bottom: 24px;
    }
    .eyebrow {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      color: var(--brand);
      font-size: 13px;
      font-weight: 800;
      letter-spacing: .14em;
      text-transform: uppercase;
    }
    .eyebrow::before { content: ''; width: 8px; height: 8px; border-radius: 50%; background: var(--brand); box-shadow: 0 0 22px var(--brand); }
    h1 { margin: 0 0 10px; font-size: clamp(34px, 5vw, 58px); line-height: 1; letter-spacing: -0.055em; }
    h2 { margin: 0; font-size: 22px; letter-spacing: -0.02em; }
    p { margin: 0; color: var(--muted); line-height: 1.65; }
    button, input { font: inherit; }
    button {
      border: 1px solid transparent;
      border-radius: 999px;
      padding: 12px 18px;
      background: linear-gradient(135deg, var(--brand-strong), #86adff);
      color: #fff;
      font-size: 14px;
      font-weight: 800;
      cursor: pointer;
      transition: transform .18s ease, opacity .18s ease, border-color .18s ease, background .18s ease;
    }
    button:hover { transform: translateY(-1px); }
    button:disabled { opacity: .48; cursor: not-allowed; transform: none; }
    button.secondary { background: rgba(255,255,255,0.08); color: var(--text); border-color: var(--border); }
    button.danger { background: rgba(255,125,125,0.10); color: var(--danger); border-color: rgba(255,125,125,0.22); }
    .status-card {
      min-width: 190px;
      padding: 16px 18px;
      border: 1px solid var(--border);
      border-radius: 24px;
      background: rgba(255,255,255,0.07);
      box-shadow: var(--shadow);
      backdrop-filter: blur(18px);
    }
    .status-label { color: var(--faint); font-size: 12px; font-weight: 800; letter-spacing: .10em; text-transform: uppercase; }
    .status { margin-top: 8px; color: var(--brand); font-weight: 900; }
    .dashboard { display: grid; grid-template-columns: 1fr; gap: 18px; align-items: start; }
    .card {
      position: relative;
      overflow: hidden;
      border: 1px solid var(--border);
      border-radius: 32px;
      background: linear-gradient(180deg, var(--panel-strong), var(--panel));
      box-shadow: var(--shadow);
      backdrop-filter: blur(22px);
    }
    .card::before {
      content: '';
      position: absolute;
      inset: 0;
      pointer-events: none;
      background: linear-gradient(135deg, rgba(255,255,255,0.12), transparent 42%);
    }
    .card-content { position: relative; padding: 24px; }
    .upload-card { min-height: 0; }
    .upload-zone {
      display: grid;
      grid-template-columns: auto minmax(0, 1fr) minmax(260px, 0.9fr);
      align-items: center;
      gap: 18px;
      margin-top: 18px;
      padding: 20px;
      border: 1px dashed rgba(137,180,255,0.36);
      border-radius: 28px;
      background: rgba(137,180,255,0.08);
      cursor: pointer;
    }
    .upload-icon {
      display: grid;
      place-items: center;
      width: 76px;
      height: 76px;
      border-radius: 24px;
      background: linear-gradient(135deg, rgba(79,125,255,0.32), rgba(137,180,255,0.18));
      color: var(--brand);
      font-size: 34px;
    }
    input[type=file] { width: 100%; color: var(--muted); }
    input[type=file]::file-selector-button {
      margin-right: 12px;
      border: 0;
      border-radius: 999px;
      padding: 11px 15px;
      background: rgba(255,255,255,0.12);
      color: var(--text);
      font-weight: 800;
      cursor: pointer;
    }
    .upload-actions { display: grid; grid-template-columns: 1fr; gap: 10px; margin-top: 16px; }
    .message { min-height: 24px; margin-top: 14px; color: var(--muted); font-size: 14px; line-height: 1.55; }
    .toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
    .library-summary { margin-top: 6px; color: var(--muted); font-size: 14px; }
    .books {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
      gap: 14px;
    }
    .book {
      position: relative;
      display: grid;
      min-height: 226px;
      padding: 16px;
      border: 1px solid var(--border);
      border-radius: 26px;
      background: rgba(255,255,255,0.065);
      transition: transform .18s ease, border-color .18s ease, background .18s ease;
    }
    .book:hover { transform: translateY(-3px); border-color: rgba(137,180,255,0.45); background: rgba(255,255,255,0.095); }
    .book-cover {
      height: 112px;
      border-radius: 20px;
      background:
        radial-gradient(circle at 24% 20%, rgba(255,255,255,0.25), transparent 24%),
        linear-gradient(135deg, rgba(79,125,255,0.72), rgba(15,27,42,0.96) 62%, rgba(255,213,138,0.22));
      padding: 14px;
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      overflow: hidden;
    }
    .cover-mark { color: rgba(255,255,255,0.38); font-size: 13px; font-weight: 900; letter-spacing: .16em; text-transform: uppercase; }
    .cover-format {
      border: 1px solid rgba(255,255,255,0.22);
      border-radius: 999px;
      padding: 5px 9px;
      color: #fff;
      background: rgba(255,255,255,0.13);
      font-size: 12px;
      font-weight: 900;
    }
    .book-body { display: grid; gap: 8px; align-content: start; margin-top: 14px; }
    .book-title {
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      font-weight: 900;
      line-height: 1.28;
    }
    .meta { color: var(--muted); font-size: 13px; line-height: 1.45; }
    .pill-row { display: flex; flex-wrap: wrap; gap: 6px; }
    .pill { border: 1px solid var(--border); border-radius: 999px; padding: 4px 8px; color: var(--muted); font-size: 12px; font-weight: 800; }
    .book-actions { display: flex; justify-content: flex-end; align-items: end; margin-top: 14px; }
    .empty {
      grid-column: 1 / -1;
      padding: 32px;
      text-align: center;
      color: var(--muted);
      border: 1px dashed var(--border);
      border-radius: 26px;
      background: rgba(255,255,255,0.045);
    }
    @media (max-width: 900px) {
      body { padding: 20px; }
      header { grid-template-columns: 1fr; }
      .status-card { min-width: 0; }
      .upload-zone { grid-template-columns: 1fr; text-align: center; justify-items: center; }
    }
    @media (max-width: 540px) {
      body { padding: 14px; }
      .card-content { padding: 18px; }
      .toolbar { display: grid; }
      .books { grid-template-columns: 1fr; }
      button { width: 100%; }
    }
  </style>
</head>
<body>
  <main>
    <header>
      <div>
        <div class="eyebrow">Local Library</div>
        <h1>暮阅书库管理</h1>
        <p>在同一局域网内批量上传 TXT / EPUB, 并管理电视本地书库。</p>
      </div>
      <div class="status-card">
        <div class="status-label">Device status</div>
        <div id="status" class="status">正在连接电视...</div>
      </div>
    </header>

    <section class="dashboard">
      <div class="card upload-card">
        <div class="card-content">
          <h2>批量上传</h2>
          <p>可一次选择多本书。同名文件会覆盖旧版本, 并重置旧章节缓存。</p>
          <form id="uploadForm">
            <label class="upload-zone" for="fileInput">
              <div class="upload-icon">↑</div>
              <div>
                <strong>选择 TXT / EPUB 文件</strong>
                <p>支持多选, 上传完成后自动刷新书库。</p>
              </div>
              <input id="fileInput" type="file" name="file" accept=".txt,.epub" multiple required />
            </label>
            <div class="upload-actions">
              <button id="uploadButton" type="submit">上传到电视</button>
            </div>
          </form>
          <div id="uploadMessage" class="message">请选择一个或多个文件。</div>
        </div>
      </div>

      <div class="card">
        <div class="card-content">
          <div class="toolbar">
            <div>
              <h2>电视书库</h2>
              <div id="librarySummary" class="library-summary">正在同步书库状态...</div>
            </div>
            <button id="rescanButton" class="secondary" type="button">重新扫描</button>
          </div>
          <div id="books" class="books"><div class="empty">正在加载书库...</div></div>
        </div>
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
    const librarySummary = document.getElementById('librarySummary');

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
      return new Date(value).toLocaleDateString();
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

    function formatSelection() {
      const count = fileInput.files.length;
      if (!count) {
        uploadMessage.textContent = '请选择一个或多个文件。';
        return;
      }
      uploadMessage.textContent = '已选择 ' + count + ' 个文件, 点击上传到电视。';
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
        librarySummary.textContent = '共 ' + data.books.length + ' 本书, 可上传, 删除或重新扫描。';
        if (!data.books.length) {
          booksEl.innerHTML = '<div class="empty">书库为空, 请先上传 TXT 或 EPUB。</div>';
          return;
        }
        booksEl.innerHTML = data.books.map(book => [
          '<article class="book">',
          '<div class="book-cover"><span class="cover-mark">Dusk</span><span class="cover-format">' + escapeHtml(book.format) + '</span></div>',
          '<div class="book-body">',
          '<div class="book-title">' + escapeHtml(book.title) + '</div>',
          '<div class="pill-row"><span class="pill">' + escapeHtml(book.format) + '</span><span class="pill">' + formatSize(book.fileSize) + '</span></div>',
          '<div class="meta">' + escapeHtml(book.author || '未知作者') + '</div>',
          '<div class="meta">最近阅读: ' + formatTime(book.lastReadTime) + '</div>',
          '</div>',
          '<div class="book-actions"><button class="danger" type="button" data-delete="' + book.id + '">删除</button></div>',
          '</article>'
        ].join('')).join('');
      } catch (error) {
        librarySummary.textContent = '书库状态同步失败';
        booksEl.innerHTML = '<div class="empty">加载失败: ' + escapeHtml(error.message) + '</div>';
      }
    }

    fileInput.addEventListener('change', formatSelection);

    uploadForm.addEventListener('submit', async event => {
      event.preventDefault();
      if (!fileInput.files.length) return;
      uploadButton.disabled = true;
      uploadMessage.textContent = '正在上传 ' + fileInput.files.length + ' 个文件...';
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
        const result = await requestJson('/api/books/' + id, { method: 'DELETE' });
        uploadMessage.textContent = result.fileDeleted ? '已删除书籍和本地文件。' : '已从书库移除, 但本地文件可能已不存在。';
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

    val fileDeleted = withContext(Dispatchers.IO) {
        chapterRepository.replaceForBook(book.id, emptyList())
        repository.delete(book)
        runCatching {
            val file = File(book.path)
            !file.isFile || file.delete()
        }.getOrDefault(false)
    }
    call.respondText("""{"success":true,"fileDeleted":$fileDeleted}""", ContentType.Application.Json)
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
        val uploadedFilenames = mutableListOf<String>()
        val skippedFilenames = mutableListOf<String>()

        multipart.forEachPart { part ->
            try {
                val filePart = part as? PartData.FileItem ?: return@forEachPart
                val safeName = UploadFilePolicy.resolveSafeFilename(filePart.originalFileName)
                if (safeName == null) {
                    skippedFilenames += filePart.originalFileName?.takeIf { it.isNotBlank() } ?: "未命名文件"
                    return@forEachPart
                }

                val documentsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS,
                )
                val bookDir = File(documentsDir, "暮阅").also {
                    if (!it.exists()) it.mkdirs()
                }
                val destFile = File(bookDir, safeName)

                withContext(Dispatchers.IO) {
                    filePart.streamProvider().use { input ->
                        destFile.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val existing = repository.findBookByPath(destFile.absolutePath)
                    val uploadedFormat = safeName.substringAfterLast(".").uppercase()
                    val uploadedTitle = safeName.substringBeforeLast(".")
                    if (existing != null) {
                        chapterRepository.replaceForBook(existing.id, emptyList())
                        repository.update(
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
                            ),
                        )
                    } else {
                        repository.insert(
                            Book(
                                title = uploadedTitle,
                                path = destFile.absolutePath,
                                format = uploadedFormat,
                                bookKind = BookKind.fromFormat(uploadedFormat).id,
                                coverSource = CoverSource.None.id,
                                fileSize = destFile.length(),
                                totalSize = destFile.length(),
                            ),
                        )
                    }
                }
                uploadedFilenames += safeName
            } finally {
                part.dispose()
            }
        }

        if (uploadedFilenames.isEmpty()) {
            val message = if (skippedFilenames.isEmpty()) {
                "未检测到可上传的文件。"
            } else {
                "未上传成功。仅支持 TXT/EPUB 文件：${skippedFilenames.joinToString("、")}"
            }
            call.respondText(message, ContentType.Text.Plain, HttpStatusCode.BadRequest)
            return
        }

        repository.scanLocalStorage()
        val uploadMessage = buildUploadResultMessage(uploadedFilenames, skippedFilenames)
        onUploadResult(uploadMessage, System.currentTimeMillis())
        call.respondText(uploadMessage, ContentType.Text.Plain)
    } catch (error: Exception) {
        DebugLogger.e("TransferServer", "handleUpload failed", error)
        call.respondText(
            "上传失败：${error.message ?: "未知错误"}",
            ContentType.Text.Plain,
            HttpStatusCode.InternalServerError,
        )
    }
}

private fun buildUploadResultMessage(
    uploadedFilenames: List<String>,
    skippedFilenames: List<String>,
): String {
    val uploadedText = if (uploadedFilenames.size == 1) {
        "上传成功：${uploadedFilenames.first()}"
    } else {
        "上传成功：${uploadedFilenames.size} 本"
    }
    return if (skippedFilenames.isEmpty()) {
        uploadedText
    } else {
        "$uploadedText；已跳过 ${skippedFilenames.size} 个不支持的文件"
    }
}
