package com.wzl.duskreader.tv.data.metadata

import com.wzl.duskreader.tv.data.entities.BookKind
import com.wzl.duskreader.tv.data.entities.CoverSource

data class ResolvedBookMetadata(
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val coverPath: String? = null,
    val coverSource: CoverSource? = null,
    val bookKind: BookKind = BookKind.Unknown,
    val tags: List<String> = emptyList(),
    val detailPageUrl: String? = null,
)
