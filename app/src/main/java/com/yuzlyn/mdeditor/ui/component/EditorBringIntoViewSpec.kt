package com.yuzlyn.mdeditor.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import kotlin.math.abs

/**
 * 編輯區的「自動帶入畫面」規格。
 *
 * Compose 的輸入框在**取得焦點**時會丟出兩個帶入畫面的請求：
 * 1. `Modifier.focusable` 請求「把整塊輸入框帶進畫面」；
 * 2. `bringSelectionEndIntoView` 請求「把游標位置帶進畫面」。
 *
 * 一般表單沒問題，但這裡的輸入框高度等於整篇正文，而且使用者常常是「先捲到某處、再長按選字」——
 * 這兩件事都會出事：
 *
 * - 第 1 個請求的矩形比視窗還高，預設算法在「兩端都超出」時會挑比較近的那一端對齊，
 *   結果常常是對齊到頂端 → 畫面直接跳回文件開頭。
 * - 第 2 個請求用的是**當下的 selection**。長按選字時焦點先到、選取後到，
 *   這時 selection 還停在上一次的位置（多半是 0），於是「把游標帶進畫面」＝「捲到最上面」。
 *
 * 這裡的政策是：**手勢進行中一律不動**，其餘請求也只補最小距離，
 * 而且位移超過一個視窗就當成錯誤座標直接放棄——真正的游標校正由編輯器自己做。
 */
@OptIn(ExperimentalFoundationApi::class)
internal class EditorBringIntoViewSpec : BringIntoViewSpec {

    /**
     * 手指是否正按在編輯區上（按下到抬起）。
     *
     * 由 [EditorScreen] 的指標監聽維護。手勢期間的所有帶入畫面請求都不該生效：
     * 使用者已經用手指指定了要看的位置，系統再「幫忙」把游標搬回畫面只會造成跳動。
     */
    @Volatile
    var gestureInProgress: Boolean = false

    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float
    ): Float {
        if (gestureInProgress) return 0f
        if (containerSize <= 0f) return 0f
        // 請求的區塊不比視窗矮：不是游標（游標只有一行高），而是「整塊輸入框」，
        // 對齊任何一端都會是一次大跳，直接不動。
        if (size >= containerSize) return 0f

        val leading = -offset
        val trailing = offset + size - containerSize

        // 完全在視窗內：不用動。跨界（比視窗高）在上面已經擋掉了。
        val distance = when {
            leading <= 0f && trailing <= 0f -> 0f
            leading >= 0f && trailing >= 0f -> 0f
            // 回傳的是「捲動量的增量」：內容在上方要往回捲（負值），在下方要往後捲（正值）。
            leading > 0f -> -leading
            else -> trailing
        }

        // 位移超過一個視窗，說明這個請求跟使用者眼前的位置無關（例如焦點事件帶來的舊游標），
        // 這種「校正」不如不做。
        if (abs(distance) > containerSize) return 0f
        return distance
    }
}
