package com.yuzlyn.mdeditor.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yuzlyn.mdeditor.data.book.ReaderMode
import com.yuzlyn.mdeditor.data.book.ReadingTheme
import com.yuzlyn.mdeditor.data.export.CardPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private val Context.appSettingsStore by preferencesDataStore(name = "app_settings")

private object Keys {
    val LANGUAGE = stringPreferencesKey("language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val SEED_COLOR = stringPreferencesKey("seed_color")
    val LAUNCHER_GLYPH = stringPreferencesKey("launcher_glyph")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val NOTE_LAYOUT = stringPreferencesKey("note_layout")
    val NOTE_SORT = stringPreferencesKey("note_sort")
    val CARD_SHAPE = stringPreferencesKey("card_shape")
    val NOTE_FONT = stringPreferencesKey("note_font")
    val READER_FONT = stringPreferencesKey("reader_font")
    val NOTE_BACKGROUND = stringPreferencesKey("note_background")
    val NOTE_BACKGROUND_PRESET = stringPreferencesKey("note_background_preset")
    val NOTE_BACKGROUND_INK = stringPreferencesKey("note_background_ink")
    val VERTICAL_LAYOUT = booleanPreferencesKey("vertical_layout")
    val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
    val WRITING_GUIDE = stringPreferencesKey("writing_guide")
    val READER_THEME = stringPreferencesKey("reader_theme")
    val READER_VERTICAL = booleanPreferencesKey("reader_vertical")
    val READER_TEXT_SCALE = intPreferencesKey("reader_text_scale")
    val READER_MODE = stringPreferencesKey("reader_mode")
    val READER_LINE_HEIGHT = intPreferencesKey("reader_line_height")
    val READER_MARGIN = intPreferencesKey("reader_margin")
    val HOME_TARGET = stringPreferencesKey("home_target")
    val STATS_ENABLED = booleanPreferencesKey("stats_enabled")
    val STATS_TRACK_READING = booleanPreferencesKey("stats_track_reading")
    val STATS_TRACK_DOCUMENTS = booleanPreferencesKey("stats_track_documents")
    val STATS_SHOW_HEATMAP = booleanPreferencesKey("stats_show_heatmap")
    val STATS_SHOW_RANKING = booleanPreferencesKey("stats_show_ranking")
    val CARD_ENABLED = booleanPreferencesKey("card_enabled")
    val CARD_POSITION = stringPreferencesKey("card_position")
    val CARD_SHOW_AVATAR = booleanPreferencesKey("card_show_avatar")
    val CARD_SHOW_NICKNAME = booleanPreferencesKey("card_show_nickname")
    val CARD_SHOW_EMAIL = booleanPreferencesKey("card_show_email")
    val CARD_SHOW_GITHUB = booleanPreferencesKey("card_show_github")
}

class AppSettingsPreferences(context: Context) {

    companion object {
        /** 閱讀字級的可調範圍（百分比）。再小就讀不動，再大一行放不下兩個字。 */
        const val MIN_TEXT_SCALE = 70
        const val MAX_TEXT_SCALE = 200
        const val TEXT_SCALE_STEP = 10

        /** 行距倍數（十分之一）。1.4 太擠、2.1 一頁放不下幾行。 */
        const val MIN_LINE_HEIGHT_TENTHS = 13
        const val MAX_LINE_HEIGHT_TENTHS = 21
        const val LINE_HEIGHT_STEP = 1

        /** 頁邊留白範圍（dp）：12 已近滿版，36 適合單手捧讀。 */
        const val MIN_MARGIN = 12
        const val MAX_MARGIN = 40
        const val MARGIN_STEP = 4
    }

    private val store = context.applicationContext.appSettingsStore

    /**
     * DataStore 的 edit 本身是原子的，但「先讀快照、再整批寫回」不是：
     * 連續改兩個設定（例如背景色 + 排版）時，第二個會拿到過期快照把第一個蓋掉。
     * 用 Mutex 把整段序列化，使用者的每個自訂改動才真的留得住。
     */
    private val updateMutex = Mutex()

    val settings: Flow<AppSettings> = store.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences: Preferences ->
            AppSettings(
                language = AppLanguage.fromTag(preferences[Keys.LANGUAGE]),
                themeMode = runCatching {
                    ThemeMode.valueOf(preferences[Keys.THEME_MODE] ?: ThemeMode.CUSTOM_SEED.name)
                }.getOrDefault(ThemeMode.CUSTOM_SEED),
                seedColor = runCatching {
                    SeedColor.valueOf(preferences[Keys.SEED_COLOR] ?: SeedColor.LOTUS.name)
                }.getOrDefault(SeedColor.LOTUS),
                launcherGlyph = LauncherGlyph.fromId(preferences[Keys.LAUNCHER_GLYPH]),
                darkMode = runCatching {
                    DarkModeOption.valueOf(
                        preferences[Keys.DARK_MODE] ?: DarkModeOption.FOLLOW_SYSTEM.name
                    )
                }.getOrDefault(DarkModeOption.FOLLOW_SYSTEM),
                noteLayout = runCatching {
                    NoteLayout.valueOf(preferences[Keys.NOTE_LAYOUT] ?: NoteLayout.MASONRY.name)
                }.getOrDefault(NoteLayout.MASONRY),
                noteSort = runCatching {
                    NoteSort.valueOf(preferences[Keys.NOTE_SORT] ?: NoteSort.CUSTOM.name)
                }.getOrDefault(NoteSort.CUSTOM),
                cardShape = runCatching {
                    CardShape.valueOf(preferences[Keys.CARD_SHAPE] ?: CardShape.DEFAULT.name)
                }.getOrDefault(CardShape.DEFAULT),
                noteFont = preferences[Keys.NOTE_FONT] ?: "system",
                readerFont = preferences[Keys.READER_FONT] ?: "system",
                noteBackground = preferences[Keys.NOTE_BACKGROUND] ?: "",
                noteBackgroundPreset = NoteBackground.fromId(preferences[Keys.NOTE_BACKGROUND_PRESET]).id,
                noteBackgroundInk = NoteBackgroundInk.fromId(preferences[Keys.NOTE_BACKGROUND_INK]).id,
                verticalLayout = preferences[Keys.VERTICAL_LAYOUT] ?: false,
                showLineNumbers = preferences[Keys.SHOW_LINE_NUMBERS] ?: true,
                writingGuide = WritingGuide.fromId(preferences[Keys.WRITING_GUIDE]).id,
                readerTheme = ReadingTheme.fromId(preferences[Keys.READER_THEME]).id,
                readerVertical = preferences[Keys.READER_VERTICAL] ?: false,
                readerTextScale = (preferences[Keys.READER_TEXT_SCALE] ?: 100)
                    .coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE),
                readerMode = preferences[Keys.READER_MODE]
                    ?.takeIf(ReaderMode::isKnown) ?: ReaderMode.PAGED.id,
                readerLineHeightTenths = (preferences[Keys.READER_LINE_HEIGHT] ?: 17)
                    .coerceIn(MIN_LINE_HEIGHT_TENTHS, MAX_LINE_HEIGHT_TENTHS),
                readerMargin = (preferences[Keys.READER_MARGIN] ?: 20)
                    .coerceIn(MIN_MARGIN, MAX_MARGIN),
                homeTarget = HomeTarget.fromId(preferences[Keys.HOME_TARGET]),
                statsEnabled = preferences[Keys.STATS_ENABLED] ?: true,
                statsTrackReading = preferences[Keys.STATS_TRACK_READING] ?: true,
                statsTrackDocuments = preferences[Keys.STATS_TRACK_DOCUMENTS] ?: true,
                statsShowHeatmap = preferences[Keys.STATS_SHOW_HEATMAP] ?: true,
                statsShowRanking = preferences[Keys.STATS_SHOW_RANKING] ?: true,
                cardEnabled = preferences[Keys.CARD_ENABLED] ?: true,
                cardPosition = CardPosition.fromId(preferences[Keys.CARD_POSITION]).id,
                cardShowAvatar = preferences[Keys.CARD_SHOW_AVATAR] ?: true,
                cardShowNickname = preferences[Keys.CARD_SHOW_NICKNAME] ?: true,
                cardShowEmail = preferences[Keys.CARD_SHOW_EMAIL] ?: true,
                cardShowGithub = preferences[Keys.CARD_SHOW_GITHUB] ?: true
            )
        }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        updateMutex.withLock {
            val next = transform(settings.first())
            store.edit { preferences ->
                preferences[Keys.LANGUAGE] = next.language.tag
                preferences[Keys.THEME_MODE] = next.themeMode.name
                preferences[Keys.SEED_COLOR] = next.seedColor.name
                preferences[Keys.LAUNCHER_GLYPH] = next.launcherGlyph.id
                preferences[Keys.DARK_MODE] = next.darkMode.name
                preferences[Keys.NOTE_LAYOUT] = next.noteLayout.name
                preferences[Keys.NOTE_SORT] = next.noteSort.name
                preferences[Keys.CARD_SHAPE] = next.cardShape.name
                preferences[Keys.NOTE_FONT] = next.noteFont
                preferences[Keys.READER_FONT] = next.readerFont
                preferences[Keys.NOTE_BACKGROUND] = next.noteBackground
                preferences[Keys.NOTE_BACKGROUND_PRESET] = next.noteBackgroundPreset
                preferences[Keys.NOTE_BACKGROUND_INK] = next.noteBackgroundInk
                preferences[Keys.VERTICAL_LAYOUT] = next.verticalLayout
                preferences[Keys.SHOW_LINE_NUMBERS] = next.showLineNumbers
                preferences[Keys.WRITING_GUIDE] = next.writingGuide
                preferences[Keys.READER_THEME] = next.readerTheme
                preferences[Keys.READER_VERTICAL] = next.readerVertical
                preferences[Keys.READER_TEXT_SCALE] = next.readerTextScale.coerceIn(
                    MIN_TEXT_SCALE,
                    MAX_TEXT_SCALE
                )
                preferences[Keys.READER_MODE] = next.readerMode
                preferences[Keys.READER_LINE_HEIGHT] = next.readerLineHeightTenths.coerceIn(
                    MIN_LINE_HEIGHT_TENTHS,
                    MAX_LINE_HEIGHT_TENTHS
                )
                preferences[Keys.READER_MARGIN] = next.readerMargin.coerceIn(MIN_MARGIN, MAX_MARGIN)
                preferences[Keys.HOME_TARGET] = next.homeTarget.id
                preferences[Keys.STATS_ENABLED] = next.statsEnabled
                preferences[Keys.STATS_TRACK_READING] = next.statsTrackReading
                preferences[Keys.STATS_TRACK_DOCUMENTS] = next.statsTrackDocuments
                preferences[Keys.STATS_SHOW_HEATMAP] = next.statsShowHeatmap
                preferences[Keys.STATS_SHOW_RANKING] = next.statsShowRanking
                preferences[Keys.CARD_ENABLED] = next.cardEnabled
                preferences[Keys.CARD_POSITION] = next.cardPosition
                preferences[Keys.CARD_SHOW_AVATAR] = next.cardShowAvatar
                preferences[Keys.CARD_SHOW_NICKNAME] = next.cardShowNickname
                preferences[Keys.CARD_SHOW_EMAIL] = next.cardShowEmail
                preferences[Keys.CARD_SHOW_GITHUB] = next.cardShowGithub
            }
        }
    }
}
