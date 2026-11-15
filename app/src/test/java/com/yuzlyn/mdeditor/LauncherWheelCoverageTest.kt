package com.yuzlyn.mdeditor

import com.materialkolor.hct.Hct
import com.yuzlyn.mdeditor.ui.theme.LauncherPalette
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * 莫奈色轮覆盖质量。
 *
 * 色轮是「莫奈主色 → 桌面图标」之间唯一的那层近似：主色是连续的，
 * 图标只有几十套。这里用 tone 40 上的一整张色相 × 彩度网格当靶子，
 * 量「随便一张壁纸派生出主色，最近的那格到底差多少」。
 *
 * 直接读生成的 launcher_palette.xml，而不是在测试里抄一份色值 ——
 * 抄一份就等于没测生成脚本，改了桶数测试照样绿。
 */
class LauncherWheelCoverageTest {

    private fun wheel(): IntArray {
        val xml = File("src/main/res/values/launcher_palette.xml")
        assertTrue("缺少 ${xml.absolutePath}", xml.exists())
        val re = Regex("""<color name="launcher_bg_[^"]+">#([0-9A-Fa-f]{6})</color>""")
        val colors = re.findAll(xml.readText()).map { m ->
            (0xFF shl 24) or m.groupValues[1].toInt(16)
        }.toList().toIntArray()
        assertTrue("色轮为空", colors.isNotEmpty())
        return colors
    }

    /** 每一格都必须能命中自己，否则说明有重复色或匹配写错了。 */
    @Test
    fun `wheel maps onto itself`() {
        val colors = wheel()
        for (i in colors.indices) {
            val hit = LauncherPalette.nearestIndex(colors, colors[i])
            assertTrue(
                String.format(Locale.ROOT, "第 %d 格 #%06X 命中了自己以外的 #%06X",
                    i, colors[i] and 0xFFFFFF, colors[hit] and 0xFFFFFF),
                hit == i
            )
        }
    }

    /**
     * 覆盖质量：枚举 tone 40 上所有「壁纸可能给出的主色」，要求最近的图标底色
     * 感知色差不超过 [MAX_COLOR_DISTANCE]。
     *
     * 上限 4 的依据（OKLab ×100 ≈ ΔE00）：3 以内人眼看不出，5 左右并排才看得出来。
     * 图标和 App 界面永远不会并排出现，留一点余量给「重新生成后别刚好踩线」。
     */
    @Test
    fun `synthetic monet primaries stay close`() {
        val colors = wheel()
        var worst = 0.0
        var worstNote = ""

        for (hue in 0 until 360 step 10) {
            for (chroma in 18..56 step 4) {
                val target = Hct.from(hue.toDouble(), chroma.toDouble(), 40.0).toInt()
                val hit = colors[LauncherPalette.nearestIndex(colors, target)]
                val d = LauncherPalette.colorDistance(target, hit)
                if (d > worst) {
                    worst = d
                    worstNote = String.format(
                        Locale.ROOT, "hue %d chroma %d → #%06X", hue, chroma, hit and 0xFFFFFF
                    )
                }
            }
        }

        println(String.format(Locale.ROOT, "最差色差 %.2f（%s）", worst, worstNote))
        assertTrue(
            String.format(Locale.ROOT, "色轮覆盖不足：最差色差 %.2f > %.1f（%s）", worst, MAX_COLOR_DISTANCE, worstNote),
            worst <= MAX_COLOR_DISTANCE
        )
    }

    private companion object {
        const val MAX_COLOR_DISTANCE = 4.0
    }
}
