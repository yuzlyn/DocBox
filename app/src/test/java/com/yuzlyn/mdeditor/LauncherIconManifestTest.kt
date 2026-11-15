package com.yuzlyn.mdeditor

import com.yuzlyn.mdeditor.data.settings.LauncherGlyph
import com.yuzlyn.mdeditor.data.settings.SeedColor
import com.yuzlyn.mdeditor.ui.theme.LauncherWheelData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 清单里的启动器别名必须和「字形 × 配色」的组合完全对上。
 *
 * 别名是运行时用字符串拼出来的（`Launcher` + 字形标签 + 配色标签），拼错了不会编译报错，
 * 只会表现为「在设置里点了图标没变」。所以这里读真实清单，把 438 个名字逐个对一遍 ——
 * 任何一个字形或配色的增删忘了重新生成，这条测试都会红。
 */
class LauncherIconManifestTest {

    private val manifest: String by lazy { File("src/main/AndroidManifest.xml").readText() }

    private val blocks: List<String>
        get() = Regex("""<activity-alias\b[\s\S]*?</activity-alias>""")
            .findAll(manifest).map { it.value }.toList()

    private fun nameOf(block: String): String =
        Regex("""android:name="\.(Launcher[A-Za-z0-9]+)"""").find(block)!!.groupValues[1]

    @Test
    fun `别名集合等于字形乘配色`() {
        val palettes = SeedColor.entries.map { it.aliasTag } +
            LauncherWheelData.CELLS.map { it.ident }
        val expected = LauncherGlyph.entries
            .flatMap { g -> palettes.map { "Launcher" + g.aliasTag + it } }
            .toSet()

        val actual = blocks.map { nameOf(it) }.toSet()

        assertEquals("配色数不对", 73, palettes.size)
        assertEquals("别名少了：${(expected - actual).take(5)}", emptySet<String>(), expected - actual)
        assertEquals("别名多了：${(actual - expected).take(5)}", emptySet<String>(), actual - expected)
    }

    /** 启动器入口只能有一个，多一个桌面上就会多一个图标。 */
    @Test
    fun `只有一个默认入口是启用的`() {
        val enabled = blocks.filter { """android:enabled="true"""" in it }.map { nameOf(it) }
        assertEquals(listOf("LauncherXiaLotus"), enabled)
    }

    /** 每条别名都得指向真实存在的 mipmap，否则点下去会装成「图标丢失」。 */
    @Test
    fun `别名引用的图标都存在`() {
        val dir = File("src/main/res/mipmap-anydpi-v26")
        blocks.forEach { block ->
            val icon = Regex("""android:icon="@mipmap/([a-z0-9_]+)"""").find(block)!!.groupValues[1]
            assertTrue("$icon 缺少 mipmap 文件", File(dir, "$icon.xml").exists())
        }
    }
}
