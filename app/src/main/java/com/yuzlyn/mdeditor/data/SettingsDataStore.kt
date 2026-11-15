package com.yuzlyn.mdeditor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val LEGACY_PREFS_NAME = "mdeditor_saf_prefs"
private val KEY_TREE_URI = stringPreferencesKey("selected_folder_tree_uri")

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mdeditor_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME))
    }
)

class SettingsDataStore(context: Context) {

    private val appContext = context.applicationContext
    private val store: DataStore<Preferences>
        get() = appContext.settingsDataStore

    val treeUri: Flow<String?> = store.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_TREE_URI] }

    suspend fun treeUriOnce(): String? = treeUri.first()

    suspend fun setTreeUri(uri: String?) {
        store.edit { preferences ->
            if (uri == null) {
                preferences.remove(KEY_TREE_URI)
            } else {
                preferences[KEY_TREE_URI] = uri
            }
        }
    }
}
