package com.yuzlyn.mdeditor.ui.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 每列的最小可讀寬度。 */
private val MinColumnWidth = 168.dp

/** 再寬也不超過六列：卡片再窄就只剩標題讀得懂。 */
private const val MaxColumns = 6

/** 低於這個寬度（分屏、窄折疊）就退回單列，硬塞兩欄只會讓標題一直截斷。 */
private val MinTwoColumnWidth = 320.dp

/**
 * 瀑布流欄數：用「內容區實際拿到的寬度」算，不是整窗寬。
 *
 * 為什麼不用 `StaggeredGridCells.Adaptive(180.dp)`：
 * 360dp 手機扣掉左右各 12dp 邊距只剩 336dp，Adaptive 會算出 1 欄，
 * 瀑布流就退化成單列——和 LIST 長得一模一樣，切換看起來完全沒反應。
 * 168dp 讓 336dp 剛好兩欄（336 / 168 = 2），平板再按同一個基準加欄。
 *
 * 平板有側欄時，呼叫端要先把側欄寬度扣掉再傳進來，否則會多算一欄。
 */
fun masonryColumns(availableWidth: Dp): Int {
    if (availableWidth < MinTwoColumnWidth) return 1
    return (availableWidth / MinColumnWidth).toInt().coerceIn(2, MaxColumns)
}
