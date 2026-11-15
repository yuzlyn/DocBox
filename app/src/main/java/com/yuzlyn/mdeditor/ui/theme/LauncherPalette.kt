package com.yuzlyn.mdeditor.ui.theme

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 把「莫奈主色」落到色轮上最近的一格。
 *
 * 桌面图标的像素是打包进 APK 的，运行时改不了，所以只能预先烘好一批底色，
 * 再挑最接近的那个别名启用（见 [LauncherWheelData] 与 `LauncherIcon`）。这决定了三条规矩：
 *
 * 1. **按感知距离挑，不能按 RGB 欧氏距离** —— 后者把亮度差当成色相差，
 *    深蓝和深紫在 RGB 值上是「邻居」，人眼却一眼看出是两种颜色。
 * 2. **用 OKLab 而不是 CIELAB 的 ΔE76** —— ΔE76 在蓝紫区会把很小的差异放大成十几个单位
 *    （`#6C4BA8` 与 `#684FA4` 只差 4/4/4，ΔE76 却算 9.6），拿它选色会做出反直觉的决定。
 *    OKLab 的欧氏距离 ×100 与 ΔE00 基本同量级：3 以内人眼看不出，5 左右并排才看得出来。
 * 3. **不设阈值，永远返回一个结果** —— 色轮覆盖不到的极端壁纸（近灰、极艳）也得有图标，
 *    宁可略微偏色，也不能让桌面停在上一套配色上。
 *
 * 纯函数、不碰 Context，是为了能直接用单测量覆盖质量（见 LauncherWheelCoverageTest）。
 */
internal object LauncherPalette {

    /**
     * 返回 [colors] 里与 [argb] 最接近的下标。
     * 距离相同时取靠前者，保证同样输入永远同样结果。
     */
    fun nearestIndex(colors: IntArray, argb: Int): Int {
        val target = oklab(argb)
        var bestIndex = 0
        var bestDistance = Double.MAX_VALUE
        for (i in colors.indices) {
            val other = oklab(colors[i])
            val dl = target[0] - other[0]
            val da = target[1] - other[1]
            val db = target[2] - other[2]
            val d = dl * dl + da * da + db * db
            if (d < bestDistance) {
                bestDistance = d
                bestIndex = i
            }
        }
        return bestIndex
    }

    /** 两色的感知色差（OKLab 距离 ×100，与 ΔE00 同量级）。量覆盖质量用。 */
    fun colorDistance(a: Int, b: Int): Double {
        val x = oklab(a)
        val y = oklab(b)
        val dl = x[0] - y[0]
        val da = x[1] - y[1]
        val db = x[2] - y[2]
        return sqrt(dl * dl + da * da + db * db)
    }

    /** sRGB → OKLab（Björn Ottosson, 2020）。分量已 ×100，便于和 ΔE 对照。 */
    private fun oklab(argb: Int): DoubleArray {
        fun linear(channel: Int): Double {
            val s = channel / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }

        val r = linear((argb shr 16) and 0xFF)
        val g = linear((argb shr 8) and 0xFF)
        val b = linear(argb and 0xFF)

        // 线性 sRGB → LMS（锥体响应）
        val l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
        val m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
        val s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b

        // 立方根压缩：感知等距的关键一步
        val lc = Math.cbrt(l)
        val mc = Math.cbrt(m)
        val sc = Math.cbrt(s)

        val lightness = 0.2104542553 * lc + 0.7936177850 * mc - 0.0040720468 * sc
        val aAxis = 1.9779984951 * lc - 2.4285922050 * mc + 0.4505937099 * sc
        val bAxis = 0.0259040371 * lc + 0.7827717662 * mc - 0.8086757660 * sc
        return doubleArrayOf(lightness * 100, aAxis * 100, bAxis * 100)
    }
}
