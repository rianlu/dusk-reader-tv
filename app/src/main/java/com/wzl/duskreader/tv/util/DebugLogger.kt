package com.wzl.duskreader.tv.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 极简调试日志记录器：同时输出到 Logcat 和 内存（用于 UI 展示）
 */
object DebugLogger {
    private const val MAX_LOGS = 50
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("DEBUG", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        addLog("ERROR", tag, message + (throwable?.let { "\n${it.localizedMessage}" } ?: ""))
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("INFO", tag, message)
    }

    private fun addLog(level: String, tag: String, message: String) {
        val time = dateFormat.format(Date())
        val logEntry = "[$time] [$level] [$tag]: $message"
        
        val currentList = _logs.value.toMutableList()
        currentList.add(0, logEntry) // 最新的在前面
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(currentList.size - 1)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
