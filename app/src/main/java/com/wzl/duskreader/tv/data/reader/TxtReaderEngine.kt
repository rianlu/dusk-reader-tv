package com.wzl.duskreader.tv.data.reader

import java.io.File
import java.nio.charset.Charset

/**
 * 极简版 TXT 读取引擎：专注速度与兼容性
 */
class TxtReaderEngine(private val file: File) {

    /**
     * 获取文件完整内容
     * 自动处理 UTF-8 和 GBK 兼容性
     */
    fun readFullContent(): String {
        val bytes = file.readBytes()
        
        // 1. 尝试 UTF-8
        try {
            if (isProbablyUtf8(bytes)) {
                return String(bytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {}

        // 2. 回退到 GBK (中文最通用)
        return try {
            String(bytes, Charset.forName("GBK"))
        } catch (e: Exception) {
            String(bytes) // 最终兜底
        }
    }

    private fun isProbablyUtf8(data: ByteArray): Boolean {
        var i = 0
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            if (b < 0x80) { i++; continue }
            if (b in 0xC2..0xDF) {
                if (i + 1 >= data.size || (data[i + 1].toInt() and 0xC0) != 0x80) return false
                i += 2
            } else if (b in 0xE0..0xEF) {
                if (i + 2 >= data.size || (data[i + 1].toInt() and 0xC0) != 0x80 || (data[i + 2].toInt() and 0xC0) != 0x80) return false
                i += 3
            } else return false
        }
        return true
    }
}
