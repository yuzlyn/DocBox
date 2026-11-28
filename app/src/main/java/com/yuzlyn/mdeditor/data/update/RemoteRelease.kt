package com.yuzlyn.mdeditor.data.update

/**
 * GitHub release 的最小投影。
 *
 * 只留真的會用到的三樣：版本、版本頁、APK 直連。`body` 可能上百 KB，
 * 這個功能不顯示更新日誌，讀進來只是浪費記憶體；`size` 也不留——下載交給瀏覽器，
 * 它在開始下載前本來就會告訴使用者檔案多大。
 *
 * [apkUrl] 允許為 null：release 只寫了說明、沒傳附件時，仍然應該告訴使用者
 * 「有新版本」，只是下載按鈕要改開版本頁，而不是假裝下載成功。
 */
data class RemoteRelease(
    val tag: String,
    val version: AppVersion,
    val pageUrl: String,
    val apkUrl: String?
) {
    companion object {
        private const val APK_MIME = "application/vnd.android.package-archive"

        /**
         * 從 GitHub 的 `/releases/latest` 回應取出需要的欄位。
         *
         * 任一必要欄位缺失就回 null——寧可顯示「版本資訊無法辨識」，也不要拿一個
         * 拼湊出來、可能指向錯誤檔案的連結讓使用者下載。
         */
        fun fromJson(text: String): RemoteRelease? {
            val root = MiniJson.parse(text) as? Map<*, *> ?: return null
            val tag = root["tag_name"] as? String ?: return null
            val version = AppVersion.parse(tag) ?: return null
            // 只接受 https：明文連結在 Android 9+ 會被系統直接擋掉，
            // 而且更新入口是最不該留降級空間的一條路。
            val pageUrl = (root["html_url"] as? String)
                ?.takeIf { it.startsWith("https://") }
                ?: return null

            val assets = root["assets"] as? List<*> ?: emptyList<Any?>()
            val apk = assets.asSequence()
                .mapNotNull { it as? Map<*, *> }
                .firstOrNull { asset ->
                    val name = asset["name"] as? String ?: ""
                    val mime = asset["content_type"] as? String ?: ""
                    name.endsWith(".apk", ignoreCase = true) || mime == APK_MIME
                }
            val apkUrl = (apk?.get("browser_download_url") as? String)
                ?.takeIf { it.startsWith("https://") }

            return RemoteRelease(
                tag = tag,
                version = version,
                pageUrl = pageUrl,
                apkUrl = apkUrl
            )
        }
    }
}
