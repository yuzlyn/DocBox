package com.yuzlyn.mdeditor.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 檢查更新的失敗原因。資料層只帶碼，句子在 `ui/i18n/LocalizedLabels.kt` 組。
 *
 * 區分這四種不是為了顯示得更花俏，而是使用者的下一步不同：
 * 網路不通要使用者檢查連線；被限流則什麼都不用做，等一會兒就好；
 * 沒有 release 與格式看不懂，都屬於「作者那邊的問題」，重試沒有意義。
 */
enum class UpdateError { NETWORK, RATE_LIMITED, NO_RELEASE, MALFORMED }

sealed interface UpdateResult {
    data class Available(val release: RemoteRelease) : UpdateResult

    data object UpToDate : UpdateResult

    data class Failed(val error: UpdateError) : UpdateResult
}

/**
 * 向 GitHub 詢問最新 release，並與本機版本比較。
 *
 * 這是整個程式**唯一**會主動連網的地方，而且只在使用者按下「檢查更新」時執行，
 * 不在啟動、不在背景、不帶任何本機資料（只有一個固定的 User-Agent）。
 * `AndroidManifest.xml` 的 INTERNET 權限就是為它開的，別處不得再發請求。
 *
 * 刻意不引入 OkHttp：只有一個 GET、不需要連線池與攔截器，`HttpURLConnection`
 * 少一個第三方依賴，也少一份 release 要維護的 keep 規則。
 */
class UpdateChecker(
    private val endpoint: String = LATEST_RELEASE_ENDPOINT,
    private val connectTimeoutMs: Int = 6_000,
    private val readTimeoutMs: Int = 8_000
) {

    /**
     * @param installedVersionName `PackageManager` 拿到的 versionName，例如 `2.1.2`。
     */
    suspend fun check(installedVersionName: String?): UpdateResult =
        withContext(Dispatchers.IO) {
            // 本機版本讀不出來就沒得比。這種情況只會出現在清單被改壞或
            // PackageManager 異常，回 MALFORMED 比隨便當成 0.0.0 誠實。
            val installed = AppVersion.parse(installedVersionName)
                ?: return@withContext UpdateResult.Failed(UpdateError.MALFORMED)

            when (val fetched = fetch()) {
                is Fetched.Payload -> {
                    val release = RemoteRelease.fromJson(fetched.body)
                        ?: return@withContext UpdateResult.Failed(UpdateError.MALFORMED)
                    if (release.version > installed) {
                        UpdateResult.Available(release)
                    } else {
                        UpdateResult.UpToDate
                    }
                }
                Fetched.RateLimited -> UpdateResult.Failed(UpdateError.RATE_LIMITED)
                Fetched.Missing -> UpdateResult.Failed(UpdateError.NO_RELEASE)
                Fetched.Unreachable -> UpdateResult.Failed(UpdateError.NETWORK)
            }
        }

    private fun fetch(): Fetched {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                // GitHub 對沒有 User-Agent 的請求直接回 403。這一行不是禮貌，是必要條件。
                setRequestProperty("User-Agent", USER_AGENT)
            }
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Fetched.Payload(readBody(connection))
                // 403 有兩種：真的被限流（剩餘次數 0），或 UA／權限被拒。
                // 兩者給使用者的建議不同，所以要看標頭再分類。
                HttpURLConnection.HTTP_FORBIDDEN ->
                    if (isRateLimited(connection)) Fetched.RateLimited else Fetched.Unreachable
                429 -> Fetched.RateLimited
                HttpURLConnection.HTTP_NOT_FOUND -> Fetched.Missing
                else -> Fetched.Unreachable
            }
        } catch (failure: IOException) {
            Fetched.Unreachable
        } catch (failure: SecurityException) {
            // 缺 INTERNET 權限時 `openConnection` 會丟 SecurityException。
            // 正常清單不會走到這裡，但設定頁不該因為一個權限問題整頁崩掉。
            Fetched.Unreachable
        } finally {
            connection?.disconnect()
        }
    }

    /** 有上限地讀完回應。GitHub 的正常回應不到 100 KB，超過 1 MB 就是異常。 */
    private fun readBody(connection: HttpURLConnection): String {
        val stream = connection.inputStream ?: throw IOException("empty response")
        stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BODY_BYTES) throw IOException("response too large")
                output.write(buffer, 0, read)
            }
            return output.toString(Charsets.UTF_8.name())
        }
    }

    private fun isRateLimited(connection: HttpURLConnection): Boolean =
        connection.getHeaderField("x-ratelimit-remaining") == "0"

    private sealed interface Fetched {
        data class Payload(val body: String) : Fetched
        data object RateLimited : Fetched
        data object Missing : Fetched
        data object Unreachable : Fetched
    }

    companion object {
        /**
         * 公開介面，固定成常數：文件、測試與實作才有同一個來源。
         *
         * 用 `/releases/latest` 而不是列表：它會自動跳過 draft 與 prerelease，
         * 正好符合「只提示正式版」。代價是將來若發 beta，這裡檢查不到——
         * 那時要改成列表並自行篩選，不是改這個字串就行。
         */
        const val LATEST_RELEASE_ENDPOINT =
            "https://api.github.com/repos/yuzlyn/DocBox/releases/latest"

        private const val USER_AGENT = "DocBox-Android"
        private const val MAX_BODY_BYTES = 1_024 * 1_024
    }
}
