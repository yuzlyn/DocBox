package com.yuzlyn.mdeditor.ui.component

/**
 * 游標自動滾動策略。
 *
 * 這裡只回答一件事：**游標被上下邊界遮住時，最小要補多少**。
 * 傳入的座標都是視口座標（內容座標已扣掉目前的滾動值），
 * 所以「游標在可視範圍內」就一定回傳 0——不回正、不置中、不動。
 *
 * 舊版把內容座標直接和滾動值比較，又用 `scroll + cursor.top` 當目標，
 * 只要頁面往下捲過，算出來的目標就會差一整個視口，看起來就是「打字時畫面亂跳」。
 *
 * @param caretTop 游標上緣在視口上的 y
 * @param caretBottom 游標下緣在視口上的 y
 * @param safeTop 安全區上緣（頂欄／狀態列之下）
 * @param safeBottom 安全區下緣（底欄之上）
 * @return 0 表示不需要動；正數把內容往上推（往後滾），負數往回滾
 */
internal fun caretScrollDelta(
    caretTop: Float,
    caretBottom: Float,
    safeTop: Float,
    safeBottom: Float
): Float {
    // 可視高度比底欄還矮（例如橫屏或分屏），沒有安全區可言，乾脆不校正。
    if (safeBottom <= safeTop) return 0f
    return when {
        // 先處理下緣：游標被底欄蓋住時往下補，補到剛好露出下緣為止。
        caretBottom > safeBottom -> caretBottom - safeBottom
        caretTop < safeTop -> caretTop - safeTop
        else -> 0f
    }
}

/**
 * 把「目前位置 + 位移」夾回文件範圍。
 * 已經到頂或到底時回傳邊界值，避免 [androidx.compose.foundation.ScrollState.scrollTo]
 * 收到越界值後被系統再夾一次而產生回彈感。
 */
internal fun clampScrollTarget(current: Float, delta: Float, max: Float): Float {
    if (max <= 0f) return 0f
    return (current + delta).coerceIn(0f, max)
}
