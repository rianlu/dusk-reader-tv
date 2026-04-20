package com.wzl.duskreader.tv.network

import android.content.Context
import android.os.Environment
import android.util.Log
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.model.UploadFilePolicy
import com.wzl.duskreader.tv.data.repository.BookRepository
import com.wzl.duskreader.tv.util.DebugLogger
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.*
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface

class FileTransferServer(
    private val context: Context,
    private val repository: BookRepository
) {
    private var server: NettyApplicationEngine? = null
    private var isRunning = false

    /**
     * 线程安全启动：确保不会重复启动多个实例
     */
    @Synchronized
    fun start(port: Int = 8080) {
        if (isRunning) {
            DebugLogger.d("TransferServer", "服务器已经在运行中，跳过启动")
            return
        }
        
        try {
            server = embeddedServer(Netty, port = port) {
                routing {
                    get("/") {
                        call.respondHtml {
                            head {
                                title { +"暮阅 - 手机传书" }
                                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                                style {
                                    unsafe {
                                        +"""
                                        body { font-family: sans-serif; text-align: center; padding: 20px; background: #f0f2f5; }
                                        .card { background: white; padding: 30px; border-radius: 20px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); margin: 0 auto; max-width: 500px; }
                                        button { background: #6200ee; color: white; border: none; padding: 15px 40px; border-radius: 30px; cursor: pointer; font-size: 18px; width: 100%; }
                                    """
                                    }
                                }
                            }
                            body {
                                div("card") {
                                    h2 { +"暮阅·无线传书" }
                                    p { +"请选择 TXT 或 EPUB 文件发送到电视" }
                                    form(action = "/upload", encType = FormEncType.multipartFormData, method = FormMethod.post) {
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
                    }

                    post("/upload") {
                        val multipart = call.receiveMultipart()
                        var uploadedBook: Book? = null
                        var uploadError: String? = null

                        multipart.forEachPart { part ->
                            try {
                                if (part is PartData.FileItem && uploadedBook == null && uploadError == null) {
                                    val safeFileName = UploadFilePolicy.resolveSafeFilename(part.originalFileName)
                                    if (safeFileName == null) {
                                        uploadError = "仅支持 TXT/EPUB 文件，且文件名必须合法。"
                                    } else {
                                        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                                        val bookDir = File(documentsDir, "暮阅")
                                        if (!bookDir.exists()) {
                                            bookDir.mkdirs()
                                        }

                                        val destFile = File(bookDir, safeFileName)
                                        withContext(Dispatchers.IO) {
                                            part.streamProvider().use { input ->
                                                destFile.outputStream().buffered().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }

                                            val existing = repository.findBookByPath(destFile.absolutePath)
                                            uploadedBook = if (existing != null) {
                                                existing
                                            } else {
                                                val newBook = Book(
                                                    title = safeFileName.substringBeforeLast("."),
                                                    path = destFile.absolutePath,
                                                    format = safeFileName.substringAfterLast(".").uppercase(),
                                                    totalSize = destFile.length()
                                                )
                                                repository.insert(newBook)
                                                newBook
                                            }
                                        }
                                    }
                                }
                            } finally {
                                part.dispose()
                            }
                        }

                        when {
                            uploadError != null -> {
                                call.respond(HttpStatusCode.BadRequest, uploadError!!)
                            }
                            uploadedBook != null -> {
                                call.respondText("上传成功！")
                            }
                            else -> {
                                call.respond(HttpStatusCode.BadRequest, "未检测到可上传的文件。")
                            }
                        }
                    }
                }
            }.start(wait = false)
            isRunning = true
            Log.d("TransferServer", "Server started on port $port")
        } catch (e: Exception) {
            DebugLogger.e("TransferServer", "启动服务器失败", e)
        }
    }

    @Synchronized
    fun stop() {
        server?.stop(1000, 2000)
        isRunning = false
        server = null
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val ip = address.hostAddress
                        if (!ip.isNullOrBlank() && !ip.contains(":")) return ip
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}
