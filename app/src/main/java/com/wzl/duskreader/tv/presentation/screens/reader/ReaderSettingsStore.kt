package com.wzl.duskreader.tv.presentation.screens.reader

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ReaderSettings(
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val theme: ReaderTheme = ReaderTheme.ForestNight,
    val textBrightness: ReaderTextBrightness = ReaderTextBrightness.Standard,
    val lineSpacing: Float = DEFAULT_LINE_SPACING,
    val pageTurnMode: PageTurnMode = PageTurnMode.HORIZONTAL,
    val autoTurnSeconds: Int = AutoTurnInterval.DEFAULT_SECONDS,
) {
    companion object {
        const val DEFAULT_FONT_SIZE = 28
        const val DEFAULT_LINE_SPACING = 1.7f
    }
}

@Singleton
class ReaderSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): ReaderSettings {
        val themeName = prefs.getString(KEY_THEME, ReaderTheme.ForestNight.name)
        val brightnessName = prefs.getString(KEY_TEXT_BRIGHTNESS, ReaderTextBrightness.Standard.name)
        val modeName = prefs.getString(KEY_PAGE_TURN_MODE, PageTurnMode.HORIZONTAL.name)
        return ReaderSettings(
            fontSize = prefs.getInt(KEY_FONT_SIZE, ReaderSettings.DEFAULT_FONT_SIZE).coerceIn(18, 80),
            theme = enumValueOrDefault(themeName, ReaderTheme.ForestNight),
            textBrightness = enumValueOrDefault(brightnessName, ReaderTextBrightness.Standard),
            lineSpacing = prefs.getFloat(KEY_LINE_SPACING, ReaderSettings.DEFAULT_LINE_SPACING).coerceIn(1.3f, 2.4f),
            pageTurnMode = enumValueOrDefault(modeName, PageTurnMode.HORIZONTAL),
            autoTurnSeconds = prefs.getInt(KEY_AUTO_TURN_SECONDS, AutoTurnInterval.DEFAULT_SECONDS)
                .coerceIn(AutoTurnInterval.MIN_SECONDS, AutoTurnInterval.MAX_SECONDS),
        )
    }

    fun save(settings: ReaderSettings) {
        prefs.edit()
            .putInt(KEY_FONT_SIZE, settings.fontSize.coerceIn(18, 80))
            .putString(KEY_THEME, settings.theme.name)
            .putString(KEY_TEXT_BRIGHTNESS, settings.textBrightness.name)
            .putFloat(KEY_LINE_SPACING, settings.lineSpacing.coerceIn(1.3f, 2.4f))
            .putString(KEY_PAGE_TURN_MODE, settings.pageTurnMode.name)
            .putInt(
                KEY_AUTO_TURN_SECONDS,
                settings.autoTurnSeconds.coerceIn(
                    AutoTurnInterval.MIN_SECONDS,
                    AutoTurnInterval.MAX_SECONDS,
                ),
            )
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        return value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
    }

    companion object {
        private const val PREFS_NAME = "reader_settings"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_THEME = "theme"
        private const val KEY_TEXT_BRIGHTNESS = "text_brightness"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_PAGE_TURN_MODE = "page_turn_mode"
        private const val KEY_AUTO_TURN_SECONDS = "auto_turn_seconds"
    }
}
