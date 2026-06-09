package com.wzl.duskreader.tv.data.metadata

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

@Singleton
class BookMetadataResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun resolve(file: File): ResolvedBookMetadata {
        val localCover = findLocalCover(file)?.let { copyCover(file, it.extension, it.inputStream()) }
        val textMetadata = if (file.extension.equals("txt", ignoreCase = true)) extractTextMetadata(file) else ResolvedBookMetadata()
        val epubMetadata = if (file.extension.equals("epub", ignoreCase = true)) extractEpubMetadata(file) else ResolvedBookMetadata()

        return ResolvedBookMetadata(
            title = epubMetadata.title ?: textMetadata.title,
            author = epubMetadata.author ?: textMetadata.author,
            description = epubMetadata.description ?: textMetadata.description,
            coverPath = localCover ?: epubMetadata.coverPath,
            tags = epubMetadata.tags.ifEmpty { textMetadata.tags },
        )
    }

    private fun extractTextMetadata(file: File): ResolvedBookMetadata {
        val head = runCatching {
            file.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                buildString {
                    repeat(80) {
                        val line = reader.readLine() ?: return@repeat
                        appendLine(line)
                    }
                }
            }
        }.getOrElse { return ResolvedBookMetadata() }

        val title = TEXT_TITLE_REGEX.find(head)?.groupValues?.getOrNull(1)?.cleanText()
        val author = TEXT_AUTHOR_REGEX.find(head)?.groupValues?.getOrNull(1)?.cleanText()
        val description = TEXT_DESCRIPTION_REGEX.find(head)?.groupValues?.getOrNull(1)?.cleanText()
        return ResolvedBookMetadata(
            title = title?.takeIf { it.isNotBlank() },
            author = author?.takeIf { it.isNotBlank() },
            description = description?.takeIf { it.isNotBlank() },
        )
    }

    private fun extractEpubMetadata(file: File): ResolvedBookMetadata {
        return runCatching {
            ZipFile(file).use { zip ->
                val opfPath = findPackagePath(zip) ?: return@use ResolvedBookMetadata()
                val opfEntry = zip.getEntry(opfPath) ?: return@use ResolvedBookMetadata()
                val xml = zip.getInputStream(opfEntry).use { parseXml(it.readBytes()) }
                val baseDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "")
                val coverEntryName = findEpubCoverEntry(zip, xml, baseDir)
                val coverPath = coverEntryName?.let { entryName ->
                    val entry = zip.getEntry(entryName) ?: return@let null
                    val extension = entryName.substringAfterLast('.', missingDelimiterValue = "jpg")
                    zip.getInputStream(entry).use { input -> copyCover(file, extension, input) }
                }
                ResolvedBookMetadata(
                    title = xml.firstText("dc:title", "title")?.cleanText(),
                    author = xml.firstText("dc:creator", "creator")?.cleanText(),
                    description = xml.firstText("dc:description", "description")?.cleanText(),
                    coverPath = coverPath,
                )
            }
        }.getOrDefault(ResolvedBookMetadata())
    }

    private fun findPackagePath(zip: ZipFile): String? {
        val container = zip.getEntry(CONTAINER_PATH) ?: return null
        val xml = zip.getInputStream(container).use { input -> parseXml(input.readBytes()) }
        for (element in xml.elementsByLocalName("rootfile")) {
            val fullPath = element.getAttribute("full-path")
            if (fullPath.isNotBlank()) return fullPath
        }
        return null
    }

    private fun findEpubCoverEntry(zip: ZipFile, xml: org.w3c.dom.Document, baseDir: String): String? {
        val coverManifestId = xml.elementsByLocalName("meta")
            .firstOrNull { it.getAttribute("name").equals("cover", ignoreCase = true) }
            ?.getAttribute("content")
            ?.takeIf { it.isNotBlank() }

        val items = xml.elementsByLocalName("item")
        val manifestCover = items.firstOrNull { item ->
            coverManifestId != null && item.getAttribute("id") == coverManifestId
        } ?: items.firstOrNull { item ->
            item.getAttribute("properties").split(' ').any { it.equals("cover-image", ignoreCase = true) }
        } ?: items.firstOrNull { item ->
            val mediaType = item.getAttribute("media-type").lowercase(Locale.ROOT)
            val href = item.getAttribute("href").lowercase(Locale.ROOT)
            mediaType.startsWith("image/") && href.contains("cover")
        }

        val href = manifestCover?.getAttribute("href")?.takeIf { it.isNotBlank() } ?: return null
        val resolved = resolveZipPath(baseDir, href)
        return resolved.takeIf { zip.getEntry(it) != null }
    }

    private fun findLocalCover(file: File): File? {
        val dir = file.parentFile ?: return null
        val baseName = file.nameWithoutExtension
        val exactNames = LOCAL_COVER_EXTENSIONS.flatMap { extension ->
            listOf("$baseName.$extension", "cover.$extension", "Cover.$extension", "folder.$extension", "Folder.$extension")
        }
        return exactNames.asSequence()
            .map { File(dir, it) }
            .firstOrNull { it.isFile && it.length() > 0 }
    }

    private fun copyCover(bookFile: File, extension: String, input: InputStream): String? {
        val normalizedExtension = extension.lowercase(Locale.ROOT).substringBefore('?').let { ext ->
            when (ext) {
                "jpg", "jpeg", "png", "webp" -> ext
                else -> "jpg"
            }
        }
        val coverDir = File(context.filesDir, COVER_DIR_NAME).also { it.mkdirs() }
        val outputFile = File(coverDir, "${bookFile.stableHash()}.$normalizedExtension")
        return runCatching {
            outputFile.outputStream().buffered().use { output -> input.copyTo(output) }
            outputFile.absolutePath
        }.getOrNull()
    }

    private fun parseXml(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        safeSetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        safeSetFeature("http://xml.org/sax/features/external-general-entities", false)
        safeSetFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }.newDocumentBuilder().parse(String(bytes, Charsets.UTF_8).trimStart().byteInputStream())

    private fun DocumentBuilderFactory.safeSetFeature(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    private fun resolveZipPath(baseDir: String, href: String): String {
        val decoded = runCatching { URLDecoder.decode(href.substringBefore('#'), "UTF-8") }.getOrElse { href.substringBefore('#') }
        val combined = if (baseDir.isBlank()) decoded else "$baseDir/$decoded"
        val parts = ArrayDeque<String>()
        combined.split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeLast()
                else -> parts.addLast(part)
            }
        }
        return parts.joinToString("/")
    }

    private fun File.stableHash(): String {
        val raw = "$absolutePath|${length()}|${lastModified()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }.take(24)
    }

    private fun org.w3c.dom.Document.firstText(vararg tagNames: String): String? {
        val localNames = tagNames.map { it.substringAfter(':') }.toSet()
        return elementsByLocalName(*localNames.toTypedArray())
            .firstNotNullOfOrNull { it.textContent?.takeIf { text -> text.isNotBlank() } }
    }

    private fun org.w3c.dom.Document.elementsByLocalName(vararg localNames: String): List<Element> {
        val expected = localNames.toSet()
        val nodes = getElementsByTagName("*")
        return buildList {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as? Element ?: continue
                val nodeLocalName = element.localName ?: element.nodeName.substringAfter(':')
                if (nodeLocalName in expected) add(element)
            }
        }
    }

    private fun String.cleanText(): String {
        return replace(WHITESPACE_REGEX, " ").trim().take(240)
    }

    private companion object {
        private const val CONTAINER_PATH = "META-INF/container.xml"
        private const val COVER_DIR_NAME = "covers"
        private val LOCAL_COVER_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val TEXT_TITLE_REGEX = Regex("(?:^|\\n)\\s*(?:书名|小说名|作品名|title)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)
        private val TEXT_AUTHOR_REGEX = Regex("(?:^|\\n)\\s*(?:作者|author)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)
        private val TEXT_DESCRIPTION_REGEX = Regex("(?:^|\\n)\\s*(?:简介|内容简介|description)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)
    }
}

data class ResolvedBookMetadata(
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val coverPath: String? = null,
    val tags: List<String> = emptyList(),
)
