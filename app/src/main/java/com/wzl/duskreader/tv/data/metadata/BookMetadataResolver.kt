package com.wzl.duskreader.tv.data.metadata

import com.wzl.duskreader.tv.data.entities.BookKind
import com.wzl.duskreader.tv.data.entities.CoverSource
import java.io.File
import java.net.URLDecoder
import java.util.Locale
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

@Singleton
class BookMetadataResolver @Inject constructor(
    private val coverCache: BookCoverCache,
    private val openLibraryCoverResolver: OpenLibraryCoverResolver,
    private val generatedCoverFactory: GeneratedCoverFactory,
) {
    fun resolve(file: File, allowNetworkCover: Boolean = true): ResolvedBookMetadata {
        val localCover = findLocalCover(file)?.let { coverFile ->
            coverFile.inputStream().use { input -> coverCache.save(file, coverFile.extension, input) }
        }
        val textMetadata = if (file.extension.equals("txt", ignoreCase = true)) extractTextMetadata(file) else ResolvedBookMetadata()
        val epubMetadata = if (file.extension.equals("epub", ignoreCase = true)) extractEpubMetadata(file) else ResolvedBookMetadata()
        val titleForLookup = epubMetadata.title ?: textMetadata.title ?: file.nameWithoutExtension
        val authorForLookup = epubMetadata.author ?: textMetadata.author
        val openDataMetadata = if (allowNetworkCover && localCover == null && epubMetadata.coverPath == null) {
            openLibraryCoverResolver.resolve(file, titleForLookup, authorForLookup)
        } else {
            null
        }
        val generatedCoverPath = if (localCover == null && epubMetadata.coverPath == null && openDataMetadata?.coverPath == null) {
            generatedCoverFactory.generate(file, titleForLookup, authorForLookup, file.extension.uppercase(Locale.ROOT))
        } else {
            null
        }

        val coverPath = localCover ?: epubMetadata.coverPath ?: openDataMetadata?.coverPath ?: generatedCoverPath
        return ResolvedBookMetadata(
            title = epubMetadata.title ?: textMetadata.title ?: openDataMetadata?.title,
            author = epubMetadata.author ?: textMetadata.author ?: openDataMetadata?.author,
            description = epubMetadata.description ?: textMetadata.description ?: openDataMetadata?.description,
            coverPath = coverPath,
            coverSource = when {
                coverPath == null -> CoverSource.None
                localCover != null -> CoverSource.LocalFile
                epubMetadata.coverPath != null -> CoverSource.EpubEmbedded
                openDataMetadata?.coverPath != null -> CoverSource.OpenData
                generatedCoverPath != null -> CoverSource.Generated
                else -> CoverSource.Unknown
            },
            bookKind = when {
                file.extension.equals("epub", ignoreCase = true) -> BookKind.Epub
                file.extension.equals("txt", ignoreCase = true) -> BookKind.Novel
                else -> BookKind.Unknown
            },
            tags = epubMetadata.tags.ifEmpty { textMetadata.tags.ifEmpty { openDataMetadata?.tags.orEmpty() } },
            detailPageUrl = openDataMetadata?.detailPageUrl,
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
        val tags = TEXT_TAGS_REGEX.find(head)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(',', '，', ';', '；', '、')
            ?.mapNotNull { it.cleanText().takeIf(String::isNotBlank) }
            .orEmpty()
        return ResolvedBookMetadata(
            title = title?.takeIf { it.isNotBlank() },
            author = author?.takeIf { it.isNotBlank() },
            description = description?.takeIf { it.isNotBlank() },
            bookKind = BookKind.Novel,
            tags = tags,
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
                    zip.getInputStream(entry).use { input -> coverCache.save(file, extension, input) }
                }
                ResolvedBookMetadata(
                    title = xml.firstText("dc:title", "title")?.cleanText(),
                    author = xml.firstText("dc:creator", "creator")?.cleanText(),
                    description = xml.firstText("dc:description", "description")?.cleanText(),
                    coverPath = coverPath,
                    coverSource = if (coverPath != null) CoverSource.EpubEmbedded else CoverSource.None,
                    bookKind = BookKind.Epub,
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
        private val LOCAL_COVER_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val TEXT_TITLE_REGEX = Regex("(?:^|\\n)\\s*(?:书名|小说名|作品名|title)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)
        private val TEXT_AUTHOR_REGEX = Regex("(?:^|\\n)\\s*(?:作者|author)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)
        private val TEXT_DESCRIPTION_REGEX = Regex("(?:^|\\n)\\s*(?:简介|内容简介|description)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)
        private val TEXT_TAGS_REGEX = Regex("(?:^|\\n)\\s*(?:标签|分类|tags)\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)
    }
}
