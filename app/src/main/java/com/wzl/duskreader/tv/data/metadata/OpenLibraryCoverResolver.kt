package com.wzl.duskreader.tv.data.metadata

import android.graphics.BitmapFactory
import android.util.Log
import com.wzl.duskreader.tv.data.entities.BookKind
import com.wzl.duskreader.tv.data.entities.CoverSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenLibraryCoverResolver @Inject constructor(
    private val coverCache: BookCoverCache,
) {
    fun resolve(bookFile: File, title: String?, author: String?): ResolvedBookMetadata? {
        val expectedTitle = title?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val searchUrl = buildSearchUrl(expectedTitle, author)
            val response = fetch(searchUrl, MAX_JSON_BYTES)
            val json = decodeText(response.bytes, response.contentType)
            val match = OpenLibrarySearchParser.parse(json, expectedTitle, author) ?: return@runCatching null
            val coverUrl = "https://covers.openlibrary.org/b/id/${match.coverId}-L.jpg?default=false"
            val coverPath = downloadCover(bookFile, coverUrl) ?: return@runCatching null
            Log.d(TAG, "resolved open library title=$expectedTitle author=$author cover=true")
            ResolvedBookMetadata(
                title = match.title,
                author = match.authors.firstOrNull() ?: author,
                coverPath = coverPath,
                coverSource = CoverSource.OpenData,
                bookKind = if (bookFile.extension.equals("epub", ignoreCase = true)) BookKind.Epub else BookKind.Novel,
                tags = listOf("Open Library"),
                detailPageUrl = match.key?.let { key -> "https://openlibrary.org$key" },
            )
        }.onFailure { error ->
            Log.w(TAG, "resolve open library failed title=$expectedTitle author=$author error=${error.message}")
        }.getOrNull()
    }

    private fun buildSearchUrl(title: String, author: String?): String {
        val query = buildList {
            add("title=${URLEncoder.encode(title, "UTF-8")}")
            author?.takeIf { it.isNotBlank() }?.let { add("author=${URLEncoder.encode(it, "UTF-8")}") }
            add("limit=10")
            add("fields=title,author_name,cover_i,key")
        }.joinToString("&")
        return "https://openlibrary.org/search.json?$query"
    }

    private fun downloadCover(bookFile: File, imageUrl: String): String? {
        return runCatching {
            val response = fetch(imageUrl, MAX_IMAGE_BYTES)
            if (!response.bytes.isValidCoverImage()) throw IOException("Invalid open cover image")
            val extensionHint = imageUrl.extensionHint() ?: response.contentType
            coverCache.save(bookFile, extensionHint, response.bytes.inputStream())
        }.onFailure { error ->
            Log.w(TAG, "download open cover failed image=$imageUrl error=${error.message}")
        }.getOrNull()
    }

    private fun fetch(url: String, maxBytes: Int): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json,image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        }
        return connection.use { http ->
            val code = http.responseCode
            if (code !in 200..299) throw IOException("Unexpected HTTP $code")
            HttpResponse(
                bytes = http.inputStream.use { input -> input.readBytesLimited(maxBytes) },
                contentType = http.contentType,
            )
        }
    }

    private fun decodeText(bytes: ByteArray, contentType: String?): String {
        val charsetName = CONTENT_TYPE_CHARSET_REGEX.find(contentType.orEmpty())?.groupValues?.getOrNull(1)
        val charset = runCatching { Charset.forName(charsetName ?: "UTF-8") }.getOrDefault(Charsets.UTF_8)
        return bytes.toString(charset)
    }

    private fun InputStream.readBytesLimited(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) throw IOException("Response too large")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun ByteArray.isValidCoverImage(): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, size, options)
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return false
        if (width < MIN_COVER_WIDTH || height < MIN_COVER_HEIGHT) return false
        val ratio = width.toFloat() / height.toFloat()
        return ratio in MIN_COVER_RATIO..MAX_COVER_RATIO
    }

    private fun String.extensionHint(): String? {
        val path = substringBefore('?').substringBefore('#')
        return path.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
            .takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
    }

    private fun HttpURLConnection.use(block: (HttpURLConnection) -> HttpResponse): HttpResponse {
        return try {
            block(this)
        } finally {
            disconnect()
        }
    }

    private data class HttpResponse(
        val bytes: ByteArray,
        val contentType: String?,
    )

    private companion object {
        private const val TAG = "OpenBookMeta"
        private const val TIMEOUT_MS = 8_000
        private const val MAX_JSON_BYTES = 512 * 1024
        private const val MAX_IMAGE_BYTES = 6 * 1024 * 1024
        private const val MIN_COVER_WIDTH = 120
        private const val MIN_COVER_HEIGHT = 160
        private const val MIN_COVER_RATIO = 0.45f
        private const val MAX_COVER_RATIO = 0.95f
        private const val USER_AGENT = "DuskReaderTV/1.0 (OpenLibrary cover lookup; https://openlibrary.org)"
        private val CONTENT_TYPE_CHARSET_REGEX = Regex("charset=([^;]+)", RegexOption.IGNORE_CASE)
    }
}
