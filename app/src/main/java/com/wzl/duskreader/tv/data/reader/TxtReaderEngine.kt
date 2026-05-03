package com.wzl.duskreader.tv.data.reader

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * 章节级 TXT 读取引擎（业内主流商业 App 的标准做法）
 *
 * 三个核心能力：
 * 1. [detectCharset] 流式嗅探编码（采样前 64KB），支持 BOM / UTF-8 启发 / GB18030 回退
 * 2. [scanChapters] 流式扫描整本，正则识别章节边界，记录每章字节偏移和长度
 * 3. [readChapterText] 按字节偏移定点读取单章内容（通常 5-50KB）
 *
 * 全流程不会把整本书一次性加载到内存，可以处理几十 MB 的大文件。
 */
class TxtReaderEngine(private val file: File) {

    /**
     * 嗅探编码：BOM > UTF-8 启发 > GB18030 回退（GB18030 是 GBK 超集，兼容更多生僻字）
     */
    fun detectCharset(): Charset {
        val sampleSize = minOf(file.length(), SNIFF_BYTES.toLong()).toInt()
        if (sampleSize == 0) return Charsets.UTF_8

        val sample = ByteArray(sampleSize)
        FileInputStream(file).use { it.read(sample) }

        // BOM 检测
        if (sampleSize >= 3 &&
            sample[0] == 0xEF.toByte() &&
            sample[1] == 0xBB.toByte() &&
            sample[2] == 0xBF.toByte()
        ) {
            return Charsets.UTF_8
        }
        if (sampleSize >= 2 && sample[0] == 0xFF.toByte() && sample[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }
        if (sampleSize >= 2 && sample[0] == 0xFE.toByte() && sample[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }

        // UTF-8 启发式（无 BOM 的纯 UTF-8 文件）
        if (isProbablyUtf8(sample)) return Charsets.UTF_8

        // 中文编码回退：GB18030 兼容性 > GBK
        return runCatching { Charset.forName("GB18030") }
            .recoverCatching { Charset.forName("GBK") }
            .getOrDefault(Charsets.UTF_8)
    }

    /**
     * 流式扫描章节边界。每行只读一次，按 \n 切行后用正则识别章节标题。
     * 章节边界对齐到行起点（即 \n 之后第一个字节），保证不会切坏多字节字符。
     *
     * @return 章节列表，至少包含 1 项（无章节标记时返回单章「正文」）
     */
    fun scanChapters(charset: Charset): List<ChapterScanResult> {
        val chapters = mutableListOf<ChapterScanResult>()
        val byteBuf = ByteArrayOutputStream(256)
        var lineStart = 0L
        var bytePos = 0L

        BufferedInputStream(FileInputStream(file), STREAM_BUFFER_BYTES).use { bis ->
            while (true) {
                val b = bis.read()
                if (b < 0) break
                bytePos++
                if (b == 0x0A) {
                    val lineStr = decodeLine(byteBuf, charset)
                    if (looksLikeChapter(lineStr)) {
                        chapters += ChapterScanResult(
                            title = lineStr.take(40),
                            byteOffset = lineStart,
                        )
                    }
                    byteBuf.reset()
                    lineStart = bytePos
                } else {
                    byteBuf.write(b)
                }
            }
            // 处理最后一行（无 \n 结尾）
            if (byteBuf.size() > 0) {
                val lineStr = decodeLine(byteBuf, charset)
                if (looksLikeChapter(lineStr)) {
                    chapters += ChapterScanResult(title = lineStr.take(40), byteOffset = lineStart)
                }
            }
        }

        // 兜底：未识别到任何章节 → 整本作单章
        if (chapters.isEmpty()) {
            return listOf(ChapterScanResult(title = "正文", byteOffset = 0L))
        }
        // 第一个章节之前还有内容（前言/序言之类未匹配上） → 补一个「开始」
        if (chapters.first().byteOffset > 0L) {
            return listOf(ChapterScanResult(title = "开始", byteOffset = 0L)) + chapters
        }
        return chapters
    }

    /**
     * 按字节偏移和长度读取单章内容。
     * 章节边界已由扫描阶段对齐到行边界，这里直接 substring 解码即可。
     */
    fun readChapterText(byteOffset: Long, byteLength: Int, charset: Charset): String {
        if (byteLength <= 0) return ""
        val buf = ByteArray(byteLength)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(byteOffset)
            val read = raf.read(buf)
            if (read < byteLength) {
                return String(buf, 0, read.coerceAtLeast(0), charset)
            }
        }
        return String(buf, charset)
    }

    private fun decodeLine(buf: ByteArrayOutputStream, charset: Charset): String =
        String(buf.toByteArray(), charset).trimEnd('\r').trim()

    private fun looksLikeChapter(line: String): Boolean {
        if (line.isEmpty() || line.length > 60) return false
        return CHAPTER_REGEX.containsMatchIn(line)
    }

    private fun isProbablyUtf8(data: ByteArray): Boolean {
        var i = 0
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            when {
                b < 0x80 -> i++
                b in 0xC2..0xDF -> {
                    if (i + 1 >= data.size || (data[i + 1].toInt() and 0xC0) != 0x80) return false
                    i += 2
                }
                b in 0xE0..0xEF -> {
                    if (i + 2 >= data.size ||
                        (data[i + 1].toInt() and 0xC0) != 0x80 ||
                        (data[i + 2].toInt() and 0xC0) != 0x80
                    ) return false
                    i += 3
                }
                b in 0xF0..0xF4 -> {
                    if (i + 3 >= data.size ||
                        (data[i + 1].toInt() and 0xC0) != 0x80 ||
                        (data[i + 2].toInt() and 0xC0) != 0x80 ||
                        (data[i + 3].toInt() and 0xC0) != 0x80
                    ) return false
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }

    companion object {
        private const val SNIFF_BYTES = 64 * 1024
        private const val STREAM_BUFFER_BYTES = 64 * 1024

        // 业内通用章节正则：覆盖中文常见章节词 + 英文 Chapter
        private val CHAPTER_REGEX = Regex(
            "^(?:" +
                "第[0-9零一二三四五六七八九十百千万两]{1,8}[章节回部集卷篇折]|" +
                "序章|序言|序幕|楔子|前言|引子|后记|尾声|番外|" +
                "Chapter\\s+[0-9IVXLCDM]+|" +
                "CHAPTER\\s+[0-9IVXLCDM]+" +
                ").{0,40}$",
            RegexOption.IGNORE_CASE,
        )
    }
}

/** 章节扫描原始结果（尚未携带 byteLength，由 caller 在汇总时根据下一章 offset 计算） */
data class ChapterScanResult(
    val title: String,
    val byteOffset: Long,
)
