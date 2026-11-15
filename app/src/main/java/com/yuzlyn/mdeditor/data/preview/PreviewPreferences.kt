package com.yuzlyn.mdeditor.data.preview

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private val Context.previewDataStore by preferencesDataStore(name = "preview_settings")

private object Keys {
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val SHOW_TOC = booleanPreferencesKey("show_toc")
    val SYNTAX_HIGHLIGHT = booleanPreferencesKey("syntax_highlight")
    val HEADING_ANCHORS = booleanPreferencesKey("heading_anchors")
    val MATH = booleanPreferencesKey("math")
    val FLAVOR = stringPreferencesKey("flavor")
}

class PreviewPreferences(context: Context) {

    private val store = context.applicationContext.previewDataStore

    /** 同 AppSettingsPreferences：把「讀快照 → 寫整批」序列化，避免連續調整時互相覆蓋。 */
    private val updateMutex = Mutex()

    val config: Flow<PreviewConfig> = store.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            PreviewConfig(
                fontScale = prefs[Keys.FONT_SCALE] ?: 1f,
                showToc = prefs[Keys.SHOW_TOC] ?: true,
                syntaxHighlight = prefs[Keys.SYNTAX_HIGHLIGHT] ?: true,
                headingAnchors = prefs[Keys.HEADING_ANCHORS] ?: false,
                math = prefs[Keys.MATH] ?: true,
                flavor = runCatching {
                    PreviewFlavor.valueOf(prefs[Keys.FLAVOR] ?: PreviewFlavor.GITHUB.name)
                }.getOrDefault(PreviewFlavor.GITHUB)
            )
        }

    suspend fun update(transform: (PreviewConfig) -> PreviewConfig) {
        updateMutex.withLock {
            val next = transform(config.first())
            store.edit { prefs ->
                prefs[Keys.FONT_SCALE] = next.fontScale
                prefs[Keys.SHOW_TOC] = next.showToc
                prefs[Keys.SYNTAX_HIGHLIGHT] = next.syntaxHighlight
                prefs[Keys.HEADING_ANCHORS] = next.headingAnchors
                prefs[Keys.MATH] = next.math
                prefs[Keys.FLAVOR] = next.flavor.name
            }
        }
    }
}
