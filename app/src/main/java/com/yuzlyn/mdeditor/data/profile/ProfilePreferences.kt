package com.yuzlyn.mdeditor.data.profile

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** 個人名片資料：頭像、暱稱與聯絡資訊。 */
data class ProfileData(
    val nickname: String = "",
    val email: String = "",
    val github: String = "",
    val avatarPath: String = ""
) {
    /**
     * 使用者只會填「使用者名稱」，名片上要的是完整網址。
     *
     * 三種輸入都吃：`yuzlyn`、`@yuzlyn`、`https://github.com/yuzlyn`——
     * 使用者從瀏覽器複製網址貼進來是很自然的動作，不該因此印出一行壞連結。
     */
    val githubUrl: String?
        get() = githubHandle(github)?.let { "github.com/$it" }
}

/** 從任意形式的 GitHub 輸入取出使用者名稱；空字串或只有網域時回 null。 */
internal fun githubHandle(raw: String): String? {
    var value = raw.trim()
    if (value.isEmpty()) return null
    value = value.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    if (value.startsWith("github.com/", ignoreCase = true)) {
        value = value.substring("github.com/".length)
    }
    value = value.trim().removePrefix("@").trim('/').trim()
    return value.takeIf { it.isNotEmpty() }
}

private val Context.profileStore by preferencesDataStore(name = "profile")

private object Keys {
    val NICKNAME = stringPreferencesKey("nickname")
    val EMAIL = stringPreferencesKey("email")
    val GITHUB = stringPreferencesKey("github")
    val AVATAR_PATH = stringPreferencesKey("avatar_path")
}

class ProfilePreferences(context: Context) {

    private val store = context.applicationContext.profileStore

    /** 同上：把「讀快照 → 寫整批」序列化，連續修改欄位時不會互相蓋掉。 */
    private val updateMutex = Mutex()

    val profile: Flow<ProfileData> = store.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences: Preferences ->
            ProfileData(
                nickname = preferences[Keys.NICKNAME] ?: "",
                email = preferences[Keys.EMAIL] ?: "",
                github = preferences[Keys.GITHUB] ?: "",
                avatarPath = preferences[Keys.AVATAR_PATH] ?: ""
            )
        }

    suspend fun update(transform: (ProfileData) -> ProfileData) {
        updateMutex.withLock {
            val next = transform(profile.first())
            store.edit { preferences ->
                preferences[Keys.NICKNAME] = next.nickname
                preferences[Keys.EMAIL] = next.email
                preferences[Keys.GITHUB] = next.github
                preferences[Keys.AVATAR_PATH] = next.avatarPath
            }
        }
    }
}
