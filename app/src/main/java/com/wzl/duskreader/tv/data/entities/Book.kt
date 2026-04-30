package com.wzl.duskreader.tv.data.entities

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
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val path: String,
    val coverPath: String? = null,
    val backdropPath: String? = null,
    val description: String? = null,
    val format: String,
    val tags: List<String> = emptyList(),
    val importedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val lastReadChapter: Int = 0,
    val lastReadPosition: Int = 0,
    val lastReadTime: Long = System.currentTimeMillis(),
    val totalSize: Long = 0
)

fun Book.preferredBackdropPath(): String? = when {
    !backdropPath.isNullOrBlank() -> backdropPath
    !coverPath.isNullOrBlank() -> coverPath
    else -> null
}
