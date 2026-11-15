package com.yuzlyn.mdeditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamicColorScheme
import com.materialkolor.hct.Hct
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * 一次性探针：实测启动器图标「莫奈色轮」的可覆盖范围。
 *
 * 目的是为 Android 12+ 的动态取色（Monet）准备一套**预烘焙**的图标底色：
 * 莫奈主色由壁纸决定，打包时无法预知，只能先按色相 × 彩度铺一张网格，
 * 运行时取最近邻。所以这张网格必须铺在真实主色所在的空间里 ——
 * 也就是 HCT 的 tone 40（Material 3 浅色 primary 的固定明度）。
 *
 * 输出 `app/build/launcher_wheel.txt`，每行：
 * `hue|wantChroma|argbHex|gotHue|gotChroma|gotTone`
 * got* 是四舍五入回 8 位色再读回来的实际值 —— delta 越大说明该格越贴 sRGB 边界，
 * 彩度被压掉了，选层时要避开。
 */
class LauncherWheelProbeTest {

    @Test
    fun probe() {
        val sb = StringBuilder()
        for (hue in 0 until 360 step 15) {
            for (wantC in 20..56 step 4) {
                val argb = Hct.from(hue.toDouble(), wantC.toDouble(), 40.0).toInt()
                val back = Hct.fromInt(argb)
                sb.append(
                    String.format(
                        Locale.ROOT,
                        "%d|%d|#%06X|%.1f|%.1f|%.1f",
                        hue, wantC, argb and 0xFFFFFF,
                        back.hue, back.chroma, back.tone
                    )
                ).append('\n')
            }
        }

        // 七颗种子的 primary（TonalSpot 派生），莫奈色轮要避开这些，免得多出一堆近似重复项。
        val seeds = linkedMapOf(
            "lotus" to 0xFF5D8895L,
            "lavender" to 0xFF6750A4L,
            "ocean" to 0xFF00696DL,
            "forest" to 0xFF2E6B4FL,
            "clay" to 0xFF8B5000L,
            "rose" to 0xFF8E4585L,
            "slate" to 0xFF4F5B92L
        )
        sb.append("---SECTORS---\n")
        for ((k, v) in seeds) {
            val p = dynamicColorScheme(Color(v), isDark = false).primary.toArgb()
            val h = Hct.fromInt(p)
            sb.append(
                String.format(
                    Locale.ROOT, "%s|#%06X|%.1f|%.1f|%.1f\n", k, p and 0xFFFFFF, h.hue, h.chroma, h.tone
                )
            )
        }

        val out = File("build/launcher_wheel.txt").apply { parentFile?.mkdirs() }
        out.writeText(sb.toString())
        println("wrote ${out.absolutePath} (${sb.lines().size} lines)")
    }
}
