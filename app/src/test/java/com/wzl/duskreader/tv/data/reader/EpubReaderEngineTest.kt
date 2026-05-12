package com.wzl.duskreader.tv.data.reader

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EpubReaderEngineTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun scanChapters_readsSpineOrderAndTitles() {
        val epub = createEpub(
            "OPS/chapter2.xhtml" to "<html><body><h1>第二章</h1><p>后读内容</p></body></html>",
            "OPS/chapter1.xhtml" to "<html><body><h1>第一章</h1><p>先读内容</p></body></html>",
        )

        val chapters = EpubReaderEngine(epub).scanChapters()

        assertEquals(listOf("第一章", "第二章"), chapters.map { it.title })
        assertEquals(listOf(0L, 1L), chapters.map { it.byteOffset })
    }

    @Test
    fun readChapterText_stripsHtmlAndDecodesEntities() {
        val epub = createEpub(
            "OPS/chapter1.xhtml" to """
                <html><head><title>Ignored</title></head><body>
                    <h1>第一章</h1>
                    <p>暮阅 &amp; EPUB&nbsp;阅读</p>
                    <p>第二段&#65281;</p>
                </body></html>
            """.trimIndent(),
        )

        val text = EpubReaderEngine(epub).readChapterText(0)

        assertTrue(text.contains("第一章"))
        assertTrue(text.contains("暮阅 & EPUB 阅读"))
        assertTrue(text.contains("第二段！"))
    }

    private fun createEpub(vararg chapters: Pair<String, String>): File {
        val epub = temp.newFile("book.epub")
        ZipOutputStream(epub.outputStream()).use { zip ->
            zip.putText(
                "META-INF/container.xml",
                """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                        <rootfiles>
                            <rootfile full-path="OPS/package.opf" media-type="application/oebps-package+xml"/>
                        </rootfiles>
                    </container>
                """.trimIndent(),
            )
            zip.putText("OPS/package.opf", packageOpf(chapters.map { it.first }.sorted()))
            chapters.forEach { (path, content) -> zip.putText(path, content) }
        }
        return epub
    }

    private fun packageOpf(paths: List<String>): String {
        val manifest = paths.mapIndexed { index, path ->
            val href = path.removePrefix("OPS/")
            "<item id=\"c$index\" href=\"$href\" media-type=\"application/xhtml+xml\"/>"
        }.joinToString("\n")
        val spine = paths.indices.joinToString("\n") { index -> "<itemref idref=\"c$index\"/>" }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version="3.0" xmlns="http://www.idpf.org/2007/opf">
                <manifest>$manifest</manifest>
                <spine>$spine</spine>
            </package>
        """.trimIndent()
    }

    private fun ZipOutputStream.putText(path: String, text: String) {
        putNextEntry(ZipEntry(path))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
