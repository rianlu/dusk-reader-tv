package com.wzl.duskreader.tv.network

import android.os.Environment
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.UploadFilePolicy
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
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
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
import kotlinx.coroutines.withContext
import kotlinx.html.ButtonType
import kotlinx.html.FormEncType
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.small
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.html.unsafe

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
) {
    companion object {
        const val DEFAULT_PORT = 8080
        private const val TAG = "TransferServer"
        private const val BOOK_DIR_NAME = "暮阅"
    }

    private var server: NettyApplicationEngine? = null
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

    fun start(port: Int = DEFAULT_PORT): TransferServerSnapshot = refresh(port)

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
            server = embeddedServer(Netty, port = port) {
                routing {
                    get("/") { call.respondHtml { renderUploadPage() } }
                    post("/upload") {
                        handleUpload(
                            call = call,
                            repository = repo,
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

private fun HTML.renderUploadPage() {
    head {
        title { +"暮阅 · 无线传书" }
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        style {
            unsafe {
                +"""
                :root {
                    color-scheme: light;
                    --bg: #eef1f5;
                    --card: rgba(255,255,255,0.92);
                    --text: #18212a;
                    --subtle: #5c6a78;
                    --brand: #2049d8;
                    --brand-dark: #1738aa;
                    --border: rgba(24,33,42,0.08);
                }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                    background:
                        radial-gradient(circle at top left, rgba(32,73,216,0.14), transparent 30%),
                        linear-gradient(180deg, #f7f9fb 0%, var(--bg) 100%);
                    color: var(--text);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                }
                .card {
                    width: min(100%, 560px);
                    background: var(--card);
                    border: 1px solid var(--border);
                    border-radius: 28px;
                    padding: 28px;
                    box-shadow: 0 22px 48px rgba(20, 28, 40, 0.12);
                    backdrop-filter: blur(16px);
                }
                h2 {
                    margin: 0 0 10px;
                    font-size: 30px;
                }
                p {
                    margin: 0 0 18px;
                    color: var(--subtle);
                    line-height: 1.6;
                }
                .hint {
                    margin: 18px 0 22px;
                    padding: 14px 16px;
                    border-radius: 18px;
                    background: rgba(32,73,216,0.08);
                    color: #2740a0;
                    font-size: 14px;
                }
                input[type=file] {
                    width: 100%;
                    padding: 18px;
                    border-radius: 18px;
                    border: 1px dashed rgba(24,33,42,0.18);
                    background: rgba(255,255,255,0.7);
                    margin-bottom: 18px;
                }
                button {
                    width: 100%;
                    border: none;
                    border-radius: 18px;
                    padding: 16px 24px;
                    background: linear-gradient(135deg, var(--brand), #5b7cff);
                    color: white;
                    font-size: 17px;
                    font-weight: 600;
                    cursor: pointer;
                }
                button:active { background: var(--brand-dark); }
                ul {
                    margin: 18px 0 0;
                    padding-left: 20px;
                    color: var(--subtle);
                    line-height: 1.7;
                }
                small {
                    display: block;
                    margin-top: 18px;
                    color: var(--subtle);
                }
                """.trimIndent()
            }
        }
    }
    body {
        div("card") {
            h2 { +"暮阅 · 无线传书" }
            p { +"选择 TXT 或 EPUB 文件发送到电视，上传完成后书库会自动刷新。" }
            div("hint") {
                +"同名文件会直接覆盖更新，适合重新导入修订后的版本。"
            }
            form(
                action = "/upload",
                encType = FormEncType.multipartFormData,
                method = FormMethod.post,
            ) {
                input(type = kotlinx.html.InputType.file, name = "file") {
                    attributes["accept"] = ".txt,.epub"
                    attributes["required"] = "true"
                }
                button(type = ButtonType.submit) { +"发送到电视" }
            }
            ul {
                li { +"确保手机/电脑和电视连接在同一局域网。" }
                li { +"上传后可回到电视首页或书库查看新增书籍。" }
                li { +"如果浏览器打不开，请返回电视端刷新传书服务状态。" }
            }
            small { +"支持格式：TXT / EPUB" }
        }
    }
}

private suspend fun handleUpload(
    call: ApplicationCall,
    repository: BookRepository,
    onUploadResult: (message: String, atMillis: Long) -> Unit,
) {
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
                uploadedBook = if (existing != null) {
                    existing.copy(
                        title = safeName.substringBeforeLast("."),
                        format = safeName.substringAfterLast(".").uppercase(),
                        fileSize = destFile.length(),
                        totalSize = destFile.length(),
                        lastReadPosition = 0,
                        lastReadTime = System.currentTimeMillis(),
                    ).also { repository.update(it) }
                } else {
                    Book(
                        title = safeName.substringBeforeLast("."),
                        path = destFile.absolutePath,
                        format = safeName.substringAfterLast(".").uppercase(),
                        fileSize = destFile.length(),
                        totalSize = destFile.length(),
                    ).also { repository.insert(it) }
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
}
