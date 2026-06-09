package com.wzl.duskreader.tv.data.entities

import java.util.Locale

enum class BookKind(
    val id: String,
    val label: String,
) {
    Novel("NOVEL", "TXT"),
    Epub("EPUB", "EPUB"),
    Unknown("UNKNOWN", "未知");

    companion object {
        fun fromId(id: String?): BookKind = entries.firstOrNull { it.id == id } ?: Unknown

        fun fromFormat(format: String): BookKind {
            return when (format.uppercase(Locale.ROOT)) {
                "EPUB" -> Epub
                "TXT" -> Novel
                else -> Unknown
            }
        }
    }
}

enum class CoverSource(
    val id: String,
    val label: String,
) {
    LocalFile("LOCAL_FILE", "本地封面"),
    EpubEmbedded("EPUB_EMBEDDED", "EPUB内置"),
    OpenData("OPEN_DATA", "开放源"),
    Generated("GENERATED", "生成封面"),
    OnlineDetail("ONLINE_DETAIL", "旧在线封面"),
    None("NONE", "无封面"),
    Unknown("UNKNOWN", "未知来源");

    companion object {
        fun fromId(id: String?): CoverSource = entries.firstOrNull { it.id == id } ?: Unknown
    }
}

fun Book.kind(): BookKind = BookKind.fromId(bookKind).takeUnless { it == BookKind.Unknown } ?: BookKind.fromFormat(format)

fun Book.coverSourceLabel(): String {
    return if (coverPath.isNullOrBlank()) {
        CoverSource.None.label
    } else {
        CoverSource.fromId(coverSource).label
    }
}

fun Book.hasOpenDataCover(): Boolean = !coverPath.isNullOrBlank() && CoverSource.fromId(coverSource) == CoverSource.OpenData

fun Book.hasGeneratedCover(): Boolean = !coverPath.isNullOrBlank() && CoverSource.fromId(coverSource) == CoverSource.Generated
