package com.wzl.duskreader.tv.data.reader

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * TXT 阅读引擎：负责高效解析和处理 TXT 文本文件
 * 针对电视端大文件加载进行了优化：
 * 1. 自动编码识别 (UTF-8 / GBK)
 * 2. 基于正则表达式的智能章节切分
 * 3. 内存友好的流式访问
 */
class TxtReaderEngine(private val file: File) {

    // 常用章节匹配正则：支持“第一章”、“第1章”、“第一回”等
    private val chapterPattern = Pattern.compile("^\\s*第[0-9一二三四五六七八九十百千万]+[章节卷集部篇回].*", Pattern.MULTILINE)

    data class Chapter(
        val title: String,      // 章节标题
        val startPos: Long,     // 在文件中的字节起始位置
        var endPos: Long = -1   // 在文件中的字节结束位置
    )

    /**
     * 自动识别文件编码
     * 简单的启发式算法：尝试 UTF-8，如果不符合则回退到 GBK (中文小说最常见的编码)
     */
    private fun detectCharset(): Charset {
        val buffer = ByteArray(4096)
        try {
            file.inputStream().use { it.read(buffer) }
        } catch (e: Exception) {
            return Charsets.UTF_8
        }
        
        return if (isUtf8(buffer)) Charsets.UTF_8 else Charset.forName("GBK")
    }

    private fun isUtf8(data: ByteArray): Boolean {
        var i = 0
        val len = data.size
        while (i < len) {
            val b = data[i].toInt() and 0xFF
            if (b < 0x80) { i++; continue }
            if (b in 0xC2..0xDF) {
                if (i + 1 >= len || (data[i + 1].toInt() and 0xC0) != 0x80) return false
                i += 2
            } else if (b in 0xE0..0xEF) {
                if (i + 2 >= len || (data[i + 1].toInt() and 0xC0) != 0x80 || (data[i + 2].toInt() and 0xC0) != 0x80) return false
                i += 3
            } else {
                return false
            }
        }
        return true
    }

    /**
     * 扫描文件并生成章节目录
     * 这是一个耗时操作，建议在后台线程执行
     */
    fun parseChapters(): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val charset = detectCharset()
        
        chapters.add(Chapter("开始", 0))

        RandomAccessFile(file, "r").use { raf ->
            val size = raf.length()
            val bufferSize = 1024 * 64
            val buffer = ByteArray(bufferSize)
            var currentPos = 0L

            while (currentPos < size) {
                raf.seek(currentPos)
                val bytesRead = raf.read(buffer)
                if (bytesRead <= 0) break

                val chunk = String(buffer, 0, bytesRead, charset)
                val matcher = chapterPattern.matcher(chunk)
                
                while (matcher.find()) {
                    // 精确计算匹配到的字符在文件中的字节位置
                    // 注意：直接截取 chunk 并转换为字节数组可能因编码问题产生微小误差，
                    // 但对于章节定位通常足够。
                    val relativeByteOffset = chunk.substring(0, matcher.start()).toByteArray(charset).size
                    val matchInFilePos = currentPos + relativeByteOffset
                    
                    if (matchInFilePos > chapters.last().startPos) {
                        chapters.last().endPos = matchInFilePos
                        chapters.add(Chapter(matcher.group().trim(), matchInFilePos))
                    }
                }
                
                // 确保 currentPos 总是以步进的方式增加，避免死循环
                // 步进长度至少应超过由于 overlap 预留的长度（如果有）
                val step = if (bytesRead > 500) bytesRead - 500L else bytesRead.toLong()
                currentPos += if (step > 0) step else 1L
            }
            if (chapters.isNotEmpty()) {
                chapters.last().endPos = size
            }
        }
        return chapters
    }

    /**
     * 读取特定章节的内容
     */
    fun readChapterContent(chapter: Chapter): String {
        if (chapter.endPos <= chapter.startPos) return ""
        
        val charset = detectCharset()
        val length = (chapter.endPos - chapter.startPos).toInt()
        val data = ByteArray(length)
        
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(chapter.startPos)
            raf.readFully(data)
        }
        
        return String(data, charset)
    }
}
