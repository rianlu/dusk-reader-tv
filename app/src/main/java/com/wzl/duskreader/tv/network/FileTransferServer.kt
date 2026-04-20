package com.wzl.duskreader.tv.network

import android.content.Context
import android.os.Environment
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.UploadFilePolicy
import com.wzl.duskreader.tv.data.repositories.BookRepository
import com.wzl.duskreader.tv.util.DebugLogger
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.ButtonType
import kotlinx.html.FormEncType
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.input
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.io.File
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BookRepository,
) {
    companion object {
        const val DEFAULT_PORT = 8080
        private const val TAG = "TransferServer"
        private const val BOOK_DIR_NAME = "暮阅"
    }

    private var server: NettyApplicationEngine? = null
    private var isRunning = false

    @Synchronized
    fun start(port: Int = DEFAULT_PORT) {
        if (isRunning) {
            DebugLogger.d(TAG, "Server already running, skip")
            return
        }
        try {
            val repo = repository
            server = embeddedServer(Netty, port = port) {
                routing {
                    get("/") { call.respondHtml { renderUploadPage() } }
                    post("/upload") { handleUpload(call, repo) }
                }
            }.start(wait = false)
            isRunning = true
            DebugLogger.i(TAG, "Server started on port $port")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Failed to start server", e)
        }
    }

    @Synchronized
    fun stop() {
        server?.stop(1000, 2000)
        isRunning = false
        server = null
    }

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
}

private fun HTML.renderUploadPage() {
    head {
        title { +"暮阅 · 无线传书" }
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        style {
            unsafe {
                +"""
                body { font-family: -apple-system, sans-serif; text-align: center; padding: 20px; background: #f0f2f5; }
                .card { background: white; padding: 30px; border-radius: 20px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); margin: 0 auto; max-width: 500px; }
                h2 { color: #333; margin-bottom: 12px; }
                p { color: #666; margin-bottom: 24px; }
                input[type=file] { width: 100%; margin-bottom: 20px; }
                button { background: #6200ee; color: white; border: none; padding: 15px 40px; border-radius: 30px; cursor: pointer; font-size: 18px; width: 100%; }
                button:active { background: #4a00b4; }
                """.trimIndent()
            }
        }
    }
    body {
        div("card") {
            h2 { +"暮阅 · 无线传书" }
            p { +"请选择 TXT 或 EPUB 文件发送到电视" }
            form(
                action = "/upload",
                encType = FormEncType.multipartFormData,
                method = FormMethod.post,
            ) {
                input(type = InputType.file, name = "file") {
                    attributes["accept"] = ".txt,.epub"
                    attributes["required"] = "true"
                }
                br; br
                button(type = ButtonType.submit) { +"立即发送" }
            }
        }
    }
}

private suspend fun handleUpload(call: ApplicationCall, repository: BookRepository) {
    val multipart = call.receiveMultipart()
    var uploadedBook: Book? = null
    var uploadError: String? = null

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

            withContext(Dispatchers.IO) {
                filePart.streamProvider().use { input ->
                    destFile.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
                val existing = repository.findBookByPath(destFile.absolutePath)
                uploadedBook = existing ?: Book(
                    title = safeName.substringBeforeLast("."),
                    path = destFile.absolutePath,
                    format = safeName.substringAfterLast(".").uppercase(),
                    fileSize = destFile.length(),
                    totalSize = destFile.length(),
                ).also { repository.insert(it) }
            }
        } finally {
            part.dispose()
        }
    }

    when {
        uploadError != null -> call.respond(HttpStatusCode.BadRequest, uploadError!!)
        uploadedBook != null -> {
            repository.scanLocalStorage()
            call.respondText("上传成功！", ContentType.Text.Plain)
        }
        else -> call.respond(HttpStatusCode.BadRequest, "未检测到可上传的文件。")
    }
}
