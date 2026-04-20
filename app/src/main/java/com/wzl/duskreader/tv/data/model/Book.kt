package com.wzl.duskreader.tv.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍实体类：添加 @Immutable 以优化 Compose 重绘性能
 */
@Immutable
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,               // 书籍在数据库中的唯一标识，自增 ID
    
    val title: String,              // 书籍标题
    val author: String? = null,     // 作者（可能为空）
    val path: String,               // 本地文件系统中的完整路径或 WebDAV 链接
    val coverPath: String? = null,  // 封面图片的本地缓存路径
    val format: String,             // 文件格式：如 "TXT", "EPUB"
    
    val lastReadChapter: Int = 0,   // 最后阅读的章节索引
    val lastReadPosition: Int = 0,  // 最后阅读的具体位置（字符偏移量或页码）
    val lastReadTime: Long = System.currentTimeMillis(), // 最后一次阅读的时间戳，用于排序
    val totalSize: Long = 0         // 文件总大小，用于进度计算
)
