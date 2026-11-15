package com.yuzlyn.mdeditor

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme
import org.junit.Test
import java.io.File

/**
 * 一次性探针：用项目实际使用的 MaterialKolor（TonalSpot）从各颗种子派生 M3 角色色。
 * 输出写到项目根 _seedprobe.txt，供设计图标时取真实色值，避免手填「看起来差不多」的颜色。
 */
class SeedProbeTest {

    private fun Color.hex(): String {
        val v = (value shr 32).toLong() and 0xFFFFFFFFL
        return String.format("#%06X", v and 0xFFFFFF)
    }

    private fun lum(c: Color): Double {
        fun ch(x: Float): Double {
            val s = x.toDouble()
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }

    private fun ratio(a: Color, b: Color): Double {
        val l1 = lum(a); val l2 = lum(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test
    fun probe() {
        val seeds = linkedMapOf(
            "LAVENDER(现行默认种子)" to 0xFF6750A4L,
            "OCEAN" to 0xFF00696DL,
            "FOREST" to 0xFF2E6B4FL,
            "CLAY" to 0xFF8B5000L,
            "ROSE" to 0xFF8E4585L,
            "SLATE" to 0xFF4F5B92L,
            "莫奈睡莲-深青绿" to 0xFF486160L,
            "莫奈睡莲-水光青" to 0xFF5D8895L,
            "莫奈睡莲-沙金" to 0xFFF3D7AAL,
            "莫奈干草堆-橄榄" to 0xFF7C734AL,
        )

        val sb = StringBuilder()
        for ((name, argb) in seeds) {
            for (dark in listOf(false, true)) {
                val cs = dynamicColorScheme(Color(argb), isDark = dark)
                val line = buildString {
                    append(name).append(" | ").append(if (dark) "DARK " else "LIGHT")
                    append(" | primary=").append(cs.primary.hex())
                    append(" onPrimary=").append(cs.onPrimary.hex())
                    append(" | primaryContainer=").append(cs.primaryContainer.hex())
                    append(" onPrimaryContainer=").append(cs.onPrimaryContainer.hex())
                    append(" | surface=").append(cs.surface.hex())
                    append(" | 对比=").append(String.format("%.2f", ratio(cs.primary, cs.onPrimary)))
                }
                sb.appendLine(line)
            }
        }
        // 写到模块 build/ 下，不污染项目根目录。
        val out = File("build/seedprobe.txt").apply { parentFile?.mkdirs() }
        out.writeText(sb.toString())
        println(sb.toString())
    }
}
