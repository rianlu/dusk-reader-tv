package com.wzl.duskreader.tv.data.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class GeneratedCoverFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun generate(bookFile: File, title: String, author: String?, format: String): String? {
        val outputDir = File(context.filesDir, COVER_DIR_NAME).also { it.mkdirs() }
        val outputFile = File(outputDir, "${bookFile.stableHash()}-generated.png")
        return runCatching {
            val bitmap = Bitmap.createBitmap(COVER_WIDTH, COVER_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawBackground(canvas, title)
            drawContent(canvas, title, author, format)
            outputFile.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
            bitmap.recycle()
            outputFile.takeIf { it.length() > 0 }?.absolutePath
        }.getOrNull()
    }

    private fun drawBackground(canvas: Canvas, seed: String) {
        val colors = PALETTES[seed.stableIndex(PALETTES.size)]
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                COVER_WIDTH.toFloat(),
                COVER_HEIGHT.toFloat(),
                colors[0],
                colors[1],
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, COVER_WIDTH.toFloat(), COVER_HEIGHT.toFloat(), paint)
        val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(48, 255, 255, 255) }
        canvas.drawCircle(COVER_WIDTH * 0.84f, COVER_HEIGHT * 0.18f, 180f, overlay)
        canvas.drawCircle(COVER_WIDTH * 0.14f, COVER_HEIGHT * 0.86f, 220f, overlay)
    }

    private fun drawContent(canvas: Canvas, title: String, author: String?, format: String) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (title.length <= 8) 56f else 46f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = 28f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(54, 255, 255, 255) }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }

        val titleLines = title.wrap(maxCharsPerLine = if (title.length <= 8) 4 else 6, maxLines = 5)
        var y = 170f
        for (line in titleLines) {
            canvas.drawTextCentered(line, y, titlePaint)
            y += titlePaint.textSize * 1.25f
        }

        author?.takeIf { it.isNotBlank() }?.let {
            canvas.drawTextCentered(it.take(18), COVER_HEIGHT - 132f, bodyPaint)
        }

        val badge = format.uppercase().take(4)
        val badgeRect = android.graphics.RectF(34f, 34f, 132f, 78f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)
        canvas.drawTextCenteredInRect(badge, badgeRect, badgeTextPaint)
    }

    private fun Canvas.drawTextCentered(text: String, baselineY: Float, paint: Paint) {
        val width = paint.measureText(text)
        drawText(text, (COVER_WIDTH - width) / 2f, baselineY, paint)
    }

    private fun Canvas.drawTextCenteredInRect(text: String, rect: android.graphics.RectF, paint: Paint) {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val x = rect.left + (rect.width() - bounds.width()) / 2f
        val y = rect.top + (rect.height() + bounds.height()) / 2f
        drawText(text, x, y, paint)
    }

    private fun String.wrap(maxCharsPerLine: Int, maxLines: Int): List<String> {
        val clean = replace(Regex("\\s+"), "").ifBlank { "未命名" }
        val lines = mutableListOf<String>()
        var index = 0
        while (index < clean.length && lines.size < maxLines) {
            val end = (index + maxCharsPerLine).coerceAtMost(clean.length)
            lines += clean.substring(index, end)
            index = end
        }
        if (index < clean.length && lines.isNotEmpty()) {
            lines[lines.lastIndex] = lines.last().dropLast(max(1, lines.last().length.coerceAtMost(2))) + "…"
        }
        return lines
    }

    private fun String.stableIndex(size: Int): Int {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return (digest.first().toInt() and 0xff) % size
    }

    private fun File.stableHash(): String {
        val raw = "$absolutePath|${length()}|${lastModified()}|generated"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }.take(24)
    }

    private companion object {
        private const val COVER_WIDTH = 480
        private const val COVER_HEIGHT = 640
        private const val COVER_DIR_NAME = "covers"
        private val PALETTES = listOf(
            intArrayOf(Color.rgb(45, 79, 255), Color.rgb(145, 73, 255)),
            intArrayOf(Color.rgb(23, 151, 122), Color.rgb(18, 72, 129)),
            intArrayOf(Color.rgb(176, 86, 34), Color.rgb(95, 35, 129)),
            intArrayOf(Color.rgb(39, 62, 84), Color.rgb(10, 132, 255)),
            intArrayOf(Color.rgb(128, 52, 118), Color.rgb(224, 111, 66)),
            intArrayOf(Color.rgb(36, 100, 74), Color.rgb(119, 147, 65)),
        )
    }
}
