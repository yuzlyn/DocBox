package com.yuzlyn.mdeditor.data.update

import android.content.Context

/**
 * 已安裝版本的讀取。
 *
 * 作者頁、說明頁、設定頁的「檢查更新」都要顯示版本號，以前前兩頁各寫了一份
 * `getPackageInfo`；再複製第三次就會出現「一頁改了、兩頁沒改」。
 *
 * 失敗一律回空字串：`PackageManager` 在正常的安裝環境不會失敗，真的失敗時
 * 顯示空白也比整頁崩掉好，呼叫端自己決定要不要隱藏。
 */
object InstalledApp {

    fun versionName(context: Context): String = runCatching {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
    }.getOrNull().orEmpty()
}
