package com.yuzlyn.mdeditor.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import com.yuzlyn.mdeditor.data.settings.LauncherGlyph
import com.yuzlyn.mdeditor.data.settings.SeedColor

/**
 * 桌面图标与 App 内的「字形 + 配色」同步。
 *
 * 图标是打包进 APK 的静态资源，运行时改不了像素，能改的只有「哪个组件是启动器入口」：
 * AndroidManifest 里每个「字形 × 配色」组合都声明了一条 `activity-alias`，各自指向一份图标，
 * 这里按当前设置启用其中一条、停用其余。切换后启动器会在几百毫秒内重绘图标。
 *
 * 两个自由度、两种匹配方式：
 * - [LauncherGlyph] 是有限的六个字形，一形一份前景，直接拼资源；
 * - 配色里 [SeedColor] 是七颗固定种子，可以**精确**同色；
 *   莫奈取色的主色由壁纸决定、穷举不了，只能走 [LauncherPalette] 的色轮就近匹配。
 *
 * 三条实现约束：
 * 0. **每条别名都要同时写 `icon` 与 `roundIcon`**。ColorOS / realme 的抽屉优先读
 *    `ActivityInfo.getRoundIconResource()`，而 activity-alias 的 roundIcon **不会**
 *    从 `<application>` 继承（只认自己那一行），取不到就回落成系统默认的机器人头像。
 *    桌面上的快捷方式用的是 `icon`，所以只会出现「桌面正常、抽屉是机器人」这种怪象。
 * 1. **先启用目标，再停用其余**。顺序反了，中间那一瞬桌面没有任何入口，
 *    部分 OEM 启动器会顺势把图标挪到末页。
 * 2. **停用其余靠查询当前入口，不靠遍历固定名单**。别名一共 438 条，遍历既慢又容易漏；
 *    更要紧的是升级前的老版本用的是 `LauncherLotus` / `LauncherW00A` 那套命名，
 *    不在新名单里，不主动清掉就会在桌面留下两个图标。
 * 3. `DONT_KILL_APP` 不杀进程；代价是切换瞬间图标会先消失再出现，
 *    这是别名方案的固有表现，不是缺陷。
 */
object LauncherIcon {

    private const val TAG = "LauncherIcon"

    /** 清单里唯一 `enabled="true"` 的别名，用来解释 COMPONENT_ENABLED_STATE_DEFAULT 的真实含义。 */
    private const val DEFAULT_ALIAS = "LauncherXiaLotus"

    /** 自訂種子色：字形 + 精确同色。 */
    fun sync(context: Context, seed: SeedColor, glyph: LauncherGlyph) {
        switchTo(context, "Launcher${glyph.aliasTag}${seed.aliasTag}")
    }

    /**
     * 莫奈取色：把系统壁纸派生出的 primary 落成色轮上最近的一格，再叠上当前字形。
     *
     * 传进来的应该是**浅色**方案的 primary —— 图标只有一个，没有亮暗两版，
     * 和种子色那七套一样统一用浅色 primary 当品牌色。
     */
    fun syncMonet(context: Context, primaryArgb: Int, glyph: LauncherGlyph) {
        val cells = LauncherWheelData.CELLS
        val colors = IntArray(cells.size) { context.getColor(cells[it].colorRes) }
        val index = LauncherPalette.nearestIndex(colors, primaryArgb)
        switchTo(context, "Launcher${glyph.aliasTag}${cells[index].ident}")
    }

    /**
     * 目前啟用中的那顆桌面圖示，畫成點陣給開屏用。
     *
     * 開屏標誌必須和使用者桌面上看到的那顆**完全一樣**：字形是他選的，底色是他那套配色
     * （莫奈取色時是色輪上最近的一格）。自己用 `primary` 重畫一個圓底會差一階顏色，
     * 從系統起始視窗交棒過來的那一下就會看見色塊跳動，所以直接把別名圖示點陣化。
     *
     * 找不到入口或解不出圖示時回 null，呼叫端退回用設定裡的字形自己畫。
     */
    fun currentIconDrawable(context: Context): Drawable? = runCatching {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(context.packageName)
        val info = context.packageManager.queryIntentActivities(intent, 0).firstOrNull()
            ?: return@runCatching null
        info.loadIcon(context.packageManager)
    }.getOrNull()

    private fun switchTo(context: Context, target: String) {
        val full = "${context.packageName}.$target"
        setEnabled(context, full, true)

        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(context.packageName)
        context.packageManager.queryIntentActivities(intent, 0).forEach { info ->
            val name = info.activityInfo.name
            if (name != full) setEnabled(context, name, false)
        }
    }

    private fun setEnabled(context: Context, className: String, enabled: Boolean) {
        val pm = context.packageManager
        val cn = ComponentName(context.packageName, className)

        // 状态已经正确就不再调 setComponentEnabledSetting：即便值没变，它也会发
        // PACKAGE_CHANGED，启动器会白刷一次图标。
        val current = when (pm.getComponentEnabledSetting(cn)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> className == "${context.packageName}.$DEFAULT_ALIAS"
        }
        if (current == enabled) return

        runCatching {
            pm.setComponentEnabledSetting(
                cn,
                if (enabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP
            )
        }.onFailure {
            // 别名对不上只可能是清单没重新生成，不该为此崩掉整个 App。
            Log.w(TAG, "切换启动器别名失败：$className", it)
        }
    }
}
