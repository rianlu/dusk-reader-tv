package com.wzl.duskreader.tv.data.reader

import java.io.File
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class EpubReaderEngine(private val file: File) {

    private var cachedChapters: List<EpubChapter>? = null

    fun scanChapters(): List<ChapterScanResult> {
        return loadChapters().map { chapter ->
            ChapterScanResult(
                title = chapter.title,
                byteOffset = chapter.index.toLong(),
            )
        }
    }

    fun readChapterText(chapterIndex: Int): String {
        return loadChapters().getOrNull(chapterIndex)?.text.orEmpty()
    }

    private fun loadChapters(): List<EpubChapter> {
        cachedChapters?.let { return it }
        val loaded = ZipFile(file).use { zip ->
            val opfPath = findPackagePath(zip)
            val spineEntries = if (opfPath != null) readSpineEntries(zip, opfPath) else emptyList()
            val contentEntries = spineEntries.ifEmpty { fallbackContentEntries(zip) }
            contentEntries.mapIndexedNotNull { index, entryName ->
                val entry = zip.getEntry(entryName) ?: return@mapIndexedNotNull null
                val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val text = htmlToText(html)
                if (text.isBlank()) return@mapIndexedNotNull null
                EpubChapter(
                    index = index,
                    title = extractTitle(html).ifBlank { "第 ${index + 1} 章" },
                    text = text,
                )
            }.ifEmpty {
                listOf(EpubChapter(index = 0, title = "正文", text = "EPUB 内容为空或暂无法解析"))
            }
        }
        cachedChapters = loaded
        return loaded
    }

    private fun findPackagePath(zip: ZipFile): String? {
        val container = zip.getEntry(CONTAINER_PATH) ?: return null
        val xml = zip.getInputStream(container).use { input -> parseXml(input.readBytes()) }
        val rootfiles = xml.getElementsByTagName("rootfile")
        for (index in 0 until rootfiles.length) {
            val element = rootfiles.item(index) as? Element ?: continue
            val fullPath = element.getAttribute("full-path")
            if (fullPath.isNotBlank()) return fullPath
        }
        return null
    }

    private fun readSpineEntries(zip: ZipFile, opfPath: String): List<String> {
        val opfEntry = zip.getEntry(opfPath) ?: return emptyList()
        val xml = zip.getInputStream(opfEntry).use { input -> parseXml(input.readBytes()) }
        val baseDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "")
        val manifest = mutableMapOf<String, String>()
        val items = xml.getElementsByTagName("item")
        for (index in 0 until items.length) {
            val element = items.item(index) as? Element ?: continue
            val id = element.getAttribute("id")
            val href = element.getAttribute("href")
            val mediaType = element.getAttribute("media-type")
            if (id.isNotBlank() && href.isReadableContentHref(mediaType)) {
                manifest[id] = resolveZipPath(baseDir, href)
            }
        }

        val entries = mutableListOf<String>()
        val itemrefs = xml.getElementsByTagName("itemref")
        for (index in 0 until itemrefs.length) {
            val element = itemrefs.item(index) as? Element ?: continue
            val idref = element.getAttribute("idref")
            manifest[idref]?.let { entries += it }
        }
        return entries.distinct()
    }

    private fun fallbackContentEntries(zip: ZipFile): List<String> {
        return zip.entries().asSequence()
            .map { it.name }
            .filter { name ->
                val lower = name.lowercase(Locale.ROOT)
                lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")
            }
            .filterNot { name ->
                val lower = name.lowercase(Locale.ROOT)
                lower.contains("nav") || lower.contains("toc") || lower.contains("cover")
            }
            .sorted()
            .toList()
    }

    private fun parseXml(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }.newDocumentBuilder().parse(String(bytes, Charsets.UTF_8).trimStart().byteInputStream())

    private fun resolveZipPath(baseDir: String, href: String): String {
        val raw = href.substringBefore('#')
        val combined = if (baseDir.isBlank()) raw else "$baseDir/$raw"
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

    private fun String.isReadableContentHref(mediaType: String): Boolean {
        val lowerHref = lowercase(Locale.ROOT)
        val lowerType = mediaType.lowercase(Locale.ROOT)
        return lowerHref.endsWith(".xhtml") ||
            lowerHref.endsWith(".html") ||
            lowerHref.endsWith(".htm") ||
            lowerType.contains("xhtml") ||
            lowerType.contains("html")
    }

    private fun extractTitle(html: String): String {
        val titleMatch = TITLE_REGEX.find(html) ?: return ""
        val raw = titleMatch.groupValues.drop(1).firstOrNull { it.isNotBlank() }.orEmpty()
        return decodeEntities(stripTags(raw)).trim().take(60)
    }

    private fun htmlToText(html: String): String {
        val normalized = html
            .replace(SCRIPT_STYLE_REGEX, " ")
            .replace(HEAD_REGEX, " ")
            .replace(BLOCK_BREAK_REGEX, "\n")
            .replace(TAG_REGEX, " ")
        return decodeEntities(normalized)
            .lines()
            .map { it.replace(WHITESPACE_REGEX, " ").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun stripTags(value: String): String = value.replace(TAG_REGEX, " ")

    private fun decodeEntities(value: String): String {
        return value
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(NUMERIC_ENTITY_REGEX) { match ->
                val raw = match.groupValues[1]
                val code = if (raw.startsWith("x", ignoreCase = true)) {
                    raw.drop(1).toIntOrNull(16)
                } else {
                    raw.toIntOrNull()
                }
                code?.let { String(Character.toChars(it)) } ?: match.value
            }
    }

    companion object {
        private const val CONTAINER_PATH = "META-INF/container.xml"
        private val TITLE_REGEX = Regex("<h[1-6][^>]*>(.*?)</h[1-6]>|<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE)
        private val SCRIPT_STYLE_REGEX = Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val HEAD_REGEX = Regex("<head[^>]*>.*?</head>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val BLOCK_BREAK_REGEX = Regex("</?(p|div|section|article|br|h[1-6]|li)[^>]*>", RegexOption.IGNORE_CASE)
        private val TAG_REGEX = Regex("<[^>]+>")
        private val WHITESPACE_REGEX = Regex("[ \\t\\x0B\\f\\r]+")
        private val NUMERIC_ENTITY_REGEX = Regex("&#(x?[0-9a-fA-F]+);")
    }
}

data class EpubChapter(
    val index: Int,
    val title: String,
    val text: String,
)
