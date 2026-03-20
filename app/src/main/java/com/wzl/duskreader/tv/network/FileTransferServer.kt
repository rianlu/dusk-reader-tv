package com.wzl.duskreader.tv.network

import android.content.Context
import android.os.Environment
import android.util.Log
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.repository.BookRepository
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

/**
 * 极简局域网传书服务器 (支持公共目录保存)
 */
class FileTransferServer(
    private val context: Context,
    private val repository: BookRepository
) {
    private var server: NettyApplicationEngine? = null

    fun start(port: Int = 8080) {
        server = embeddedServer(Netty, port = port) {
            routing {
                get("/") {
                    call.respondHtml {
                        head {
                            title { +"暮阅 - 手机传书" }
                            meta(name = "viewport", content = "width=device-width, initial-scale=1")
                            style {
                                +"""
                                    body { font-family: sans-serif; text-align: center; padding: 20px; background: #f5f5f5; }
                                    .card { background: white; padding: 30px; border-radius: 15px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); margin: 0 auto; max-width: 500px; }
                                    input { margin: 20px 0; }
                                    button { background: #6200ee; color: white; border: none; padding: 12px 24px; border-radius: 5px; cursor: pointer; font-size: 16px; }
                                """
                            }
                        }
                        body {
                            div("card") {
                                h2 { +"暮阅·无线传书" }
                                p { +"请选择 TXT 或 EPUB 文件上传到电视公共文档目录" }
                                // 修正：明确使用 kotlinx.html 提供的枚举
                                form(action = "/upload", encType = FormEncType.multipartFormData, method = FormMethod.post) {
                                    input(type = InputType.file, name = "file") {
                                        attributes["accept"] = ".txt,.epub"
                                    }
                                    br
                                    button(type = ButtonType.submit) { +"立即上传" }
                                }
                                p {
                                    style = "font-size: 12px; color: #888; margin-top: 20px;"
                                    +"书籍将保存在电视公共目录下的 /Documents/暮阅 文件夹中"
                                }
                            }
                        }
                    }
                }

                post("/upload") {
                    val multipart = call.receiveMultipart()
                    var fileName = ""
                    var fileBytes: ByteArray? = null

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            fileName = part.originalFileName ?: "unknown.txt"
                            fileBytes = part.streamProvider().readBytes()
                        }
                        part.dispose()
                    }

                    if (fileBytes != null && fileName.isNotEmpty()) {
                        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        val publicDir = File(documentsDir, "暮阅")
                        if (!publicDir.exists()) {
                            publicDir.mkdirs()
                        }
                        
                        val destFile = File(publicDir, fileName)
                        destFile.writeBytes(fileBytes!!)
                        
                        withContext(Dispatchers.IO) {
                            if (repository.findBookByPath(destFile.absolutePath) == null) {
                                repository.insert(Book(
                                    title = fileName.substringBeforeLast("."),
                                    path = destFile.absolutePath,
                                    format = fileName.substringAfterLast(".").uppercase()
                                ))
                            }
                        }
                        call.respondText("上传成功！《$fileName》已存入电视公共文档目录：/Documents/暮阅/")
                    } else {
                        call.respondText("上传失败，请重试。")
                    }
                }
            }
        }.start(wait = false)
        Log.d("TransferServer", "Server started on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val ip = address.hostAddress
                        if (!ip.contains(":")) return ip
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}
