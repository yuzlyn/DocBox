package com.yuzlyn.mdeditor.data.update

/**
 * 版本號的**可比較**形式。
 *
 * 為什麼不直接比字串：`2.1.10` 的字典序小於 `2.1.9`，但它其實更新——用字串比大小，
 * 遲早會在第十個修補版上誤報「已是最新」。為什麼不拿 versionCode 比：GitHub release
 * 只給 tag（`v2.1.2`），拿不到 versionCode；而且本地 versionCode 是手寫的整數，
 * 兩個倉庫之外的東西沒法互相驗證。
 *
 * 段數不固定，缺的段補 0：`2.1` 與 `2.1.0` 視為同一版。作者少寫一段不該被當成
 * 「有新版本」，否則每次發版都會出現一次假提示。
 *
 * 預發布（`-beta.1`）沿用 SemVer 的排序：同號時正式版大於預發布版，預發布之間
 * 逐段比、數字段小於字母段。這不是為了顯示，是為了不讓「2.2.0-beta」蓋過「2.2.0」。
 */
data class AppVersion(
    val segments: List<Int>,
    /** 預發布標記（`-beta.1` 裡的 `beta.1`）；正式版為 null。 */
    val prerelease: String? = null
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val size = maxOf(segments.size, other.segments.size)
        for (index in 0 until size) {
            val mine = segments.getOrElse(index) { 0 }
            val theirs = other.segments.getOrElse(index) { 0 }
            if (mine != theirs) return mine.compareTo(theirs)
        }
        return when {
            prerelease == null && other.prerelease == null -> 0
            prerelease == null -> 1
            other.prerelease == null -> -1
            else -> comparePrerelease(prerelease, other.prerelease)
        }
    }

    /**
     * 顯示用：去掉 `v` 前綴，只留數字段（與可選的預發布後綴）。
     *
     * 對話框要寫的是「最新版本 2.2.0」，不是「v2.2.0」——`v` 是 git tag 的語法，
     * 不是版本號本身。
     */
    override fun toString(): String = buildString {
        append(segments.joinToString("."))
        if (prerelease != null) append('-').append(prerelease)
    }

    companion object {
        /**
         * 解析 `v2.1.2`、`2.1`、`2.1.2-beta.1` 這幾種寫法；其餘一律回 null。
         *
         * 寧可解析失敗也不要猜：`2.1.x`、`latest`、空字串若被硬塞成 0，
         * 比對結果會變成「已是最新」，使用者就再也看不到更新提示了。
         */
        fun parse(raw: String?): AppVersion? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            // 去掉 git tag 慣用的 v 前綴與 SemVer 的 build metadata（`+sha` 不參與比較）。
            val text = trimmed
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('+')
            if (text.isEmpty()) return null

            val core = text.substringBefore('-')
            val prerelease = text.substringAfter('-', "").ifBlank { null }
            val parts = core.split('.')
            // 四位以上不是這個專案的版本格式，當成解析失敗比亂比安全。
            if (parts.size > 4) return null

            val segments = parts.map { part ->
                // 長度上限擋住 `99999999999` 這種會讓 toInt 溢出的輸入。
                if (part.isEmpty() || part.length > 6) return null
                if (part.any { it !in '0'..'9' }) return null
                part.toInt()
            }
            return AppVersion(segments, prerelease)
        }

        private fun comparePrerelease(first: String, second: String): Int {
            val left = first.split('.')
            val right = second.split('.')
            for (index in 0 until maxOf(left.size, right.size)) {
                val a = left.getOrNull(index) ?: return -1
                val b = right.getOrNull(index) ?: return 1
                val aNumber = a.toIntOrNull()
                val bNumber = b.toIntOrNull()
                val result = when {
                    aNumber != null && bNumber != null -> aNumber.compareTo(bNumber)
                    aNumber != null -> -1
                    bNumber != null -> 1
                    else -> a.compareTo(b)
                }
                if (result != 0) return result
            }
            return 0
        }
    }
}
