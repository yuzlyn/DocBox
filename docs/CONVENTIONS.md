# DocBox 项目细则

> 本文件承载 [AGENTS.md](../AGENTS.md) 移出的细节。根文件只留硬规则与门禁，避免每次 AI 会话都吞掉大段背景。
> 最后核对：2026-11-16。

## 1. 技术栈与版本锁定

版本集中在 `gradle/libs.versions.toml`，**不要在 `app/build.gradle.kts` 里写死版本号**。

| 项目 | 版本 |
|---|---|
| Kotlin | 2.2.10 |
| AGP / Gradle | 9.2.1 / 9.4.1 |
| Compose BOM | 2026.02.01 |
| Material 3 Expressive | 1.5.0-alpha26 |
| Navigation 3 | 1.2.1 |
| Room / KSP | 2.8.5 / 2.2.10-2.0.2 |
| DataStore | 1.1.7 |
| PDFBox-Android | 2.0.27.0 |
| SDK | minSdk 26、targetSdk 36、compileSdk 37 |
| JDK | 17+（实测 17.0.19） |

`compileSdk` 被抬到 37 是因为 M3 Expressive alpha26 / Compose 1.12 声明了 `minCompileSdk 37`；`targetSdk` 按平台要求停在 36。**不要为了「对齐」把 targetSdk 也改到 37。**

`gradle.properties` 里 `android.disallowKotlinSourceSets=false` 是 AGP 9 内建 Kotlin 支持与 KSP 共存所必需，删了会构建失败。

## 2. 架构分层

```
data/     纯 Kotlin，不依赖 Compose，可直接 JVM 单测
ui/       Compose 界面 + ViewModel，通过 StateFlow 消费 data/
```

- `data/local/` Room 数据库、DAO、实体、迁移；schema 导出到 `app/schemas/`（`ksp { arg("room.schemaLocation", ...) }`）。**改实体必须提交新 schema 文件**，否则迁移测试失效。
- `data/repository/` 笔记仓储与合并规则。
- `data/book/` EPUB / MOBI / AZW3 / FB2 解析与阅读状态。
- `data/importer/` DOCX / PDF / HTML 读取器。
- `data/export/` DOCX 写出、PDF/PNG 渲染、分享卡片。
- `data/stats/` 阅读与编辑统计——刻意做成纯数据层，以便 JVM 单测。
- `ui/viewmodel/` 页面状态持有者，禁止在 Composable 里直接碰 Room。

界面导航用 Navigation 3（`ui/navigation/` 的 NavKey），入口在 `MainActivity.kt`。

## 3. Material 3 视觉规范（现行，源自 `.cursorrules`）

1. **颜色**：一律 `MaterialTheme.colorScheme.primary / surface / onSurface / surfaceContainerHighest` 等 Token。
2. **间距**：8dp 网格，只用 4 / 8 / 16 / 24 / 32 dp。
3. **圆角**：小件（Badge、Tooltip）4dp；中件（Card、Dialog、DropdownMenu）12–16dp；底部抽屉 / 全屏 28dp。
4. **图标**：只用 `androidx.compose.material.icons`（Filled / Outlined / Rounded），不自造 SVG。
5. **组件**：一律 `androidx.compose.material3.*`。M3 没有的交互，用最接近的 M3 组件组合（如长按文本菜单用 `DropdownMenu` 或 `ModalBottomSheet`）。
6. 不手写 Canvas 阴影或复杂自定义 Layout，除非标准组件确实做不到。

**已知例外**（历史原因，不再扩大）：`ui/reader/ReaderScreen.kt` 与 `EditorScreen.kt` 内有少量 `Color(0x...)` 字面量，用于位图色值转换和固定墨水色；`ui/component/SupportCodes.kt` 用品牌色。新增代码不得沿用这种做法。

## 4. i18n 工作流

`ui/i18n/AppStrings.kt` 是**唯一**的界面文案来源，四个实例：

```kotlin
val ZhTwStrings = AppStrings().apply { appName = "…" }   // 繁體中文，默认
val ZhCnStrings = AppStrings().apply { … }               // 简体中文
val EnUsStrings = AppStrings().apply { … }               // English
val FrFrStrings = AppStrings().apply { … }               // Français
val LocalAppStrings = staticCompositionLocalOf { ZhTwStrings }
```

规则：

- 加字段 → 在 `class AppStrings` 里加 `lateinit var xxx: String`（或 `(Int) -> String` 这类函数类型），四份 `apply` 同步填值。
- **绝不能改回 `data class` 或加构造参数**：359 个字段已越过 JVM 255 参数槽上限，编译能过、加载即炸，而且只在 JVM（单测）上必现。
- `AppStringsFillTest` 会校验四份都填了且无空串；`ErrorStringsTest` 覆盖错误码文案。
- 错误码 → 文案的映射在 `ui/i18n/LocalizedLabels.kt`：`bookError()` / `importError()` / `imageInsertError()` / `Throwable.bookMessage()`。
- 帮助页文案在 `ui/i18n/HelpContent.kt` 四份；作者信息集中在 `AUTHOR_*` 常量，头像命名 `author_avatar.png` 放 `res/drawable-nodpi/` 即由 `getIdentifier` 自动探测。

## 5. 数据层的错误约定

```
data/  抛错误码（enum / sealed） + 语言中立的技术细节
ui/    把错误码翻成当前语言的一句话
```

理由：数据层要能被 JVM 单测覆盖，中文句子会让测试断言绑死语言，也让四语同步变成体力活。

`Log.*` 里的中文可以保留——只给开发看。**任何会出现在屏幕上的字符串都不行。**

## 6. 构建与签名

- `release` 使用 **debug keystore**（`signingConfig = signingConfigs.getByName("debug")`），这是刻意为之：v1.0.0 起发布的 APK 都是同一张证书，老用户可以直接覆盖升级。换签名会导致必须先卸载。要正式发布到别处，请换自己的 keystore。
- `isMinifyEnabled = false`：当前未启用混淆，改这个要先跑一遍仪器测试。
- `assembleRelease` 会顺带执行 `lintVitalRelease`（只查致命问题），比 `lintDebug` 宽松得多。两者都要过：`lintVitalRelease` 绿不代表 `lintDebug` 绿。

## 7. 测试布局

- `app/src/test/` — JVM 单测，188 用例 / 25 类。覆盖解析器、排版、导出、统计、i18n 完整性。
- `app/src/androidTest/` — 仪器测试，含导出渲染套件，会把样例 PDF / PNG 写到 App 外部目录供人眼核对。
- 样本电子书：`app/src/test/resources/books/` 与 `app/src/androidTest/assets/books/` 各存一份**内容相同**的 `sample.epub` / `sample.mobi` / `sample.azw3`。
- `tools/make_test_books.py` 手工拼二进制生成上述样本（EPUB / PalmDOC / KF8），只写 `app/src/test/resources/books/`，**不会**同步到 androidTest，需要时手动复制。
- 该脚本对 EPUB 不幂等：`zipfile` 会写入当前时间戳，每次运行 `sample.epub` 字节都变。跑完记得 `git checkout -- app/src/test/resources/books/sample.epub`，否则会混进无意义的 diff。

## 8. 预览引擎

`preview-engine/` 是 WebView 预览的源码（markdown-it 14 + highlight.js + MathJax + GFM 插件），产物打进 `app/src/main/assets/preview/`。改引擎要两边一致：改源码、重新产出、确认 assets 内容同步。

渲染安全：脚本、iframe、事件处理属性、`javascript:` URL 一律剥离；源码视图始终显示原始文本。

## 9. 已知技术债

| 项 | 现状 | 备注 |
|---|---|---|
| `lintDebug` | ✅ 0 error（2026-11-16 清零） | 仍余 60 warning，不作门禁 |
| 仪器测试 | 本机无法无人值守跑通 | BRT-W09 需手动允许安装 |
| 测试样本 | 两份副本手工同步 | 有漂移风险 |
| 生成脚本 | EPUB 非幂等 | zip 时间戳 |
| 混淆 | 未启用 | `isMinifyEnabled = false` |

### 9.1 lint 清零时踩到的两个坑（别再写回去）

**坑一：`Scaffold` 的 `innerPadding` 在浮层顶栏下确实用不上。**

`LibraryScreen` 与 `MainScreen` 的顶栏是盖在内容上的，且垂直偏移是动画值
（`topBarOffset`）。`Scaffold` 给的 `innerPadding` 是静态快照，套上去会在顶栏收合时留下一条空白。
两处内容都自己算 `topInset` / `contentTopInset`，所以参数是**刻意**忽略的。
正确写法是加注释 + `@Suppress("UnusedMaterial3ScaffoldPaddingParameter")`，不是把 `_` 改名了事——
lint 只看「有没有被引用」，叫 `_` 一样报。

**坑二：`JavascriptInterface` 报的是「解析失败」，不是「真的没标注解」。**

`addJavascriptInterface(bridge, …)` 若写在 `WebView(ctx).apply { }` 里，`bridge` 是被 lambda
捕获的变量，lint 的 `ConstantEvaluator` 拿不到具体型别，会退化成泛型 `T`，然后按「一个都没标」
上报——即使方法上确实有 `@JavascriptInterface`。给变量补显式型别**没用**（实测过）。

有效做法是把桥接安装抽成带参数型别的普通函数：

```kotlin
private fun WebView.installJsBridges(bridge: PreviewBridge, clipboard: PreviewClipboard) {
    addJavascriptInterface(bridge, "PreviewBridge")
    addJavascriptInterface(clipboard, "AndroidClipboard")
}
```

参数型别明确后 lint 就能正常解析，检查得以保留。**不建议**直接 `@SuppressLint("JavascriptInterface")`
把它关掉：JS 桥接少标一个注解，方法在 API 17+ 会对 JS 完全隐形，功能静默失效，这道防线值得留着。

对照参考：`ui/reader/BookReaderView.kt` 的 `addJavascriptInterface(ReaderBridge(hooks), "Android")`
直接传构造调用，型别可解析，从未报过。

## 10. 语言约定

- 代码注释、提交说明、解释性文字：**繁體中文**（沿用既有风格）。
- 本文件与 `AGENTS.md`：简体中文，面向当前维护者。
- 界面字符串：四语，见第 4 节。
