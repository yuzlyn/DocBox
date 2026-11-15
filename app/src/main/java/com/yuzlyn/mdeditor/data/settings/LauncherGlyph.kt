package com.yuzlyn.mdeditor.data.settings

import androidx.annotation.DrawableRes
import com.yuzlyn.mdeditor.R

/**
 * 桌面图标使用的字形。
 *
 * 「匣」是原稿直描；其余五个取自同一张字形演变图（微伯簋 / 曾仲游父簋 / 魯大司徒厚氏 /
 * 說文古文 / 說文竹部），轮廓都在 `res/drawable/ic_launcher_foreground*.xml`。
 *
 * 图标是打包进 APK 的静态资源，运行时换不了像素，所以每个「字形 × 配色」组合都在
 * AndroidManifest 里有一条 activity-alias，由 `ui.theme.LauncherIcon` 启用其中一个。
 * [aliasTag] 必须与 `icon-samples/_gen_launcher_icons.py` 里的 GLYPHS 表逐字一致，
 * 拼错只会表现为「点了没反应」—— `LauncherIconManifestTest` 会把清单里的名字全对一遍。
 */
enum class LauncherGlyph(
    val id: String,
    val aliasTag: String,
    @DrawableRes val foreground: Int
) {
    XIA("xia", "Xia", R.drawable.ic_launcher_foreground),
    WEIBO("weibo", "Weibo", R.drawable.ic_launcher_foreground_weibo),
    ZENGZHONG("zengzhong", "Zengzhong", R.drawable.ic_launcher_foreground_zengzhong),
    LUSITU("lusitu", "Lusitu", R.drawable.ic_launcher_foreground_lusitu),
    SHUOWENGU("shuowengu", "Shuowengu", R.drawable.ic_launcher_foreground_shuowen_gu),
    SHUOWENZHU("shuowenzhu", "Shuowenzhu", R.drawable.ic_launcher_foreground_shuowen_zhu);

    companion object {
        fun fromId(id: String?): LauncherGlyph =
            entries.firstOrNull { it.id == id } ?: XIA
    }
}
