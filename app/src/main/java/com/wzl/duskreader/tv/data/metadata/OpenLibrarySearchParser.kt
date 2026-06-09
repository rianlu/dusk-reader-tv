package com.wzl.duskreader.tv.data.metadata

import java.util.Locale

object OpenLibrarySearchParser {
    fun parse(json: String, expectedTitle: String, expectedAuthor: String?): OpenLibraryBookMatch? {
        return DOC_REGEX.findAll(json)
            .mapNotNull { match -> parseDoc(match.value) }
            .firstOrNull { book -> book.matches(expectedTitle, expectedAuthor) }
    }

    private fun parseDoc(docJson: String): OpenLibraryBookMatch? {
        val coverId = findJsonNumber(docJson, "cover_i") ?: return null
        val title = findJsonString(docJson, "title") ?: return null
        return OpenLibraryBookMatch(
            title = title.cleanText(),
            authors = findJsonStringArray(docJson, "author_name").map { it.cleanText() },
            coverId = coverId,
            key = findJsonString(docJson, "key"),
        )
    }

    private fun OpenLibraryBookMatch.matches(expectedTitle: String, expectedAuthor: String?): Boolean {
        if (title.normalizedName() != expectedTitle.normalizedName()) return false
        val requiredAuthor = expectedAuthor?.normalizedName()?.takeIf { it.isNotBlank() } ?: return true
        return authors.any { author -> author.normalizedName() == requiredAuthor }
    }

    private fun findJsonString(json: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*"((?:\\.|[^\\"])*)"""")
        return regex.find(json)?.groupValues?.getOrNull(1)?.jsonUnescape()
    }

    private fun findJsonNumber(json: String, key: String): Int? {
        val regex = Regex(""""$key"\s*:\s*(\d+)""")
        return regex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun findJsonStringArray(json: String, key: String): List<String> {
        val regex = Regex(""""$key"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
        val body = regex.find(json)?.groupValues?.getOrNull(1) ?: return emptyList()
        return JSON_STRING_REGEX.findAll(body).map { match -> match.groupValues[1].jsonUnescape() }.toList()
    }

    private fun String.jsonUnescape(): String {
        return replaceUnicodeEscapes()
            .replace("\\/", "/")
            .replace("\\\"", "\"")
    }

    private fun String.replaceUnicodeEscapes(): String {
        return UNICODE_ESCAPE_REGEX.replace(this) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }
    }

    private fun String.cleanText(): String = replace(WHITESPACE_REGEX, " ").trim().take(240)

    private fun String.normalizedName(): String = replace(WHITESPACE_REGEX, "").trim().lowercase(Locale.ROOT)

    private val DOC_REGEX = Regex("""\{[^{}]*"cover_i"[^{}]*}""", RegexOption.DOT_MATCHES_ALL)
    private val JSON_STRING_REGEX = Regex(""""((?:\\.|[^\\"])*)"""")
    private val WHITESPACE_REGEX = Regex("""\s+""")
    private val UNICODE_ESCAPE_REGEX = Regex("""\\u([0-9a-fA-F]{4})""")
}

data class OpenLibraryBookMatch(
    val title: String,
    val authors: List<String>,
    val coverId: Int,
    val key: String?,
)
