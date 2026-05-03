package com.wzl.duskreader.tv.data.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书籍章节索引（业内章节级阅读架构的核心）
 *
 * 一本书在首次打开时被流式扫描，章节边界通过正则识别后整本写入此表。
 * Reader 后续按章节级加载（每次只解码 5-50KB），避免一次性加载整本。
 */
@Immutable
@Entity(
    tableName = "book_chapters",
    indices = [Index(value = ["bookId", "chapterIndex"])],
)
data class BookChapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val title: String,
    /** 在源文件中的字节起始偏移，用于 RandomAccessFile.seek */
    val byteOffset: Long,
    /** 章节占据的字节数（已对齐到行边界，不会切坏多字节字符） */
    val byteLength: Int,
)
