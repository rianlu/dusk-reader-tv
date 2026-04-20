package com.wzl.duskreader.tv.data.entities

object UploadFilePolicy {
    private val allowedExtensions = setOf("txt", "epub")
    private val illegalChars = Regex("[^\\p{L}\\p{N}._ -]")
    private val repeatedUnderscore = Regex("_+")
    private const val maxFileNameLength = 120

    fun resolveSafeFilename(originalFileName: String?): String? {
        if (originalFileName.isNullOrBlank()) return null

        val fileNameOnly = originalFileName
            .trim()
            .replace('\\', '/')
            .substringAfterLast('/')
            .trim()

        if (fileNameOnly.isEmpty()) return null

        val sanitized = fileNameOnly
            .replace(illegalChars, "_")
            .replace(repeatedUnderscore, "_")
            .trim()
            .take(maxFileNameLength)

        val extension = sanitized.substringAfterLast('.', "").lowercase()
        val baseName = sanitized.substringBeforeLast('.', "").trim().trim('.')

        if (baseName.isEmpty()) return null
        if (extension !in allowedExtensions) return null

        return "$baseName.$extension"
    }
}
