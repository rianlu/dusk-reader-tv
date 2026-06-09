package com.wzl.duskreader.tv.data.metadata

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookCoverCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun save(bookFile: File, extensionHint: String?, input: InputStream): String? {
        val coverDir = File(context.filesDir, COVER_DIR_NAME).also { it.mkdirs() }
        val outputFile = File(coverDir, "${bookFile.stableHash()}.${normalizeExtension(extensionHint)}")
        return runCatching {
            outputFile.outputStream().buffered().use { output -> input.copyTo(output) }
            outputFile.takeIf { it.length() > 0 }?.absolutePath
        }.getOrNull()
    }

    private fun normalizeExtension(extensionHint: String?): String {
        val normalized = extensionHint
            ?.lowercase(Locale.ROOT)
            ?.substringBefore('?')
            ?.substringBefore(';')
            ?.substringAfterLast('/')
            ?.substringAfterLast('.')
        return when (normalized) {
            "jpg", "jpeg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpg"
        }
    }

    private fun File.stableHash(): String {
        val raw = "$absolutePath|${length()}|${lastModified()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }.take(24)
    }

    private companion object {
        private const val COVER_DIR_NAME = "covers"
    }
}
