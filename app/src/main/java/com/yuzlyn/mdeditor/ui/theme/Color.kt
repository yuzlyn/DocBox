package com.yuzlyn.mdeditor.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 基線種子色（MD3 默認紫）。
 *
 * **目前沒有任何地方引用**：App 內的主色一律由 `AppSettings.seedColor` 交給 MaterialKolor 派生，
 * 這裡只作為基線記錄保留。
 *
 * 名字不要改回 `SeedColor`——`data.settings` 裡已經有一個同名枚舉，
 * 而**同包的頂層聲明會遮蔽 import**，編譯器會把 `SeedColor.LOTUS` 解析成這個 Color 值的成員，
 * 表現為「枚舉成員全部無法解析」。
 */
val BaselineSeedColor = Color(0xFF6750A4)
