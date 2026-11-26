# DocBox · 文书匣子

[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.02.01-blueviolet.svg)](https://developer.android.com/develop/ui/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Expressive-6750A4.svg)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

DocBox（中文名「文书匣子」）是 Android 上的 Markdown 笔记本和阅读器，用 Kotlin、Jetpack Compose 与 Material 3 Expressive 写成。

笔记是放在你自己文件夹里的 `.md` 文件，文件夹通过系统的文件夹授权选一次就行。App 只在本机数据库里存标题、标签、排版这些元数据，没有账号、不做同步、也不申请联网权限。导入的书留在 App 自己的目录里，用内置阅读器打开，排版与编辑器共用一套，包含直排。它同时注册了「打开方式」，别的 App 里的文件可以直接送过来。

[English](README.md)

## 功能

### 写作

- **Markdown 编辑与实时预览。** markdown-it 的 GFM 规则：表格、任务列表、脚注、`==荧光笔==`、`~下标~`、`^上标^`、定义列表、属性块；代码高亮与 MathJax 公式都在内置 WebView 里渲染。
- **内联 HTML。** `<br>`、`<span style="…">`、`<u>`、`<kbd>`、`<details>` 这类标签会生效，效果和 VS Code 的 Markdown 预览一致。script、iframe、事件属性与 `javascript:` 会被清掉，源码视图始终显示原始文本。
- **自动保存。** 停手约 400 毫秒后草稿写进 Room，约一秒后写回 `.md` 文件，切到后台时再冲一次。编辑中途进程被杀，下次打开会把草稿写回去。
- **横排／竖排。** 竖排是从上到下、从右到左的直书，字步 1.2em、栏距 1.72em 分别设定，并做避头尾处理，收尾标点不会落在栏顶。一行太长折到下一栏时仍算同一行，不会重复编号。
- **稿纸格子。** 文字下面可以铺方格或横线，铺满整个书写区——不会写到最后一个字就断掉；格子节拍与导出图片用的是同一套，编辑页和分享出去的图是同一张纸。直排时每一栏（有字的和空白的）共用同一套节拍，一栏一栏横向对得齐。
- **两种排版。** 瀑布流，或单列列表。列表会留一行摘要和日期，一列不会只剩标题；两种排版里，来源格式的小徽章都贴在标题最后一行的末尾。
- **撤销与重做**，光标会跟着滚回可见位置。

### 导入

- 支持 `.docx`、`.pdf`、`.epub`、`.mobi` / `.azw3`、`.fb2`、`.html`、`.markdown` 和纯文本。先看扩展名，再看文件头，没有扩展名的文件也能认出来。
- 每种格式各用各的读法：Word 走 OOXML，PDF 的文本层交给 PDFBox，EPUB 读 OPF／spine，HTML 转 Markdown 时保留标题、列表、链接和代码块。
- 导入的笔记卡片上标着来源格式。从 PDF 导入的笔记会留着原文件链接，编辑器可以跳回真实版面；早先的笔记可以手动补一次链接，启动检查会清掉权限已经失效的链接。
- **打开方式。** 清单里同时声明了型别与副档名，Word、PDF、HTML、Markdown、纯文本与电子书都会出现在其他 App 的「打开方式」里。电子书进阅读器，其余导入成笔记；`.docx` 会被认成文档，而不是压缩包形式的书。

### 书库与阅读器

- EPUB、MOBI、FB2 放在首页书架上，阅读进度按书记住。
- 内置阅读器支持横排／竖排、翻页或滚动，字号、行距、留白都可调，纸色跟着 Monet 走。
- 导入的字体和笔记背景在阅读器里同样可用。
- **长按多选。** 按住一本书进入选择，顶栏变成「已选 N 项」，可分享或删除。分享直接送原档，不转档、不留暂存副本，一次可以送多本。删除一律走多选，所以按下卡片时高度不会跳。
- 书库同样有瀑布流与列表两种排版，进场动效与笔记页一致。

### 分享与导出

- 支持 Markdown、纯文本、Word（`.docx`）、PDF 和 PNG。
- PDF 与 PNG 跟着笔记的方向走：直排笔记导出就是直排，有辅助线时格子与编辑器同一套。
- PNG 可以带一张个人名片。横排是左边圆形头像、右边昵称、邮箱、GitHub 三行，两边共用同一条水平中线；直排是头像在上、三栏资料在下。名片放正文前面还是后面、要不要头像、哪几项要印，都在分享面板里逐项开关，选过一次就记住。直排长文会一直往左延伸，不会被截断。
- Markdown 与纯文本导出时名片是三行文字，PDF 只印文字、不带头像。

### 阅读统计

- 阅读时长、读过的书、编辑过的文件，全部在本机算。
- 一整年的格子热力图，下面是书与文件的排行榜。
- 统计页顶部用你的昵称和头像打招呼。每一项都能在设置里单独关掉，整个关掉之后不再写入任何记录。

### 外观

- Material 3 Expressive：Monet 从壁纸取色，也可以指定种子色；深色模式、卡片形状、内置字体都在。
- 可以导入自己的 `.ttf` / `.otf`，编辑器和阅读器分别指定默认字体。导入的字体可以删掉。
- 笔记背景：走 Monet、纯色，或自己的图片（可以导入多张）。图片上的字可指定深色或浅色。
- 四种语言：繁體中文、简体中文、English、Français。可随时手动切换，也可以选「跟随系统语言」让界面跟着设备走。所有页面都已覆盖，包括错误提示。
- 平板与折叠屏在 840dp 以上宽度时，书库会切换成侧栏布局：左侧 80dp 导航栏、顶栏只留搜索框、卡片网格按宽度自动分列而不是拉宽。
- 各页面共用一套 Material 3 Expressive 转场，进页时内容依次浮上来。
- 卡片按 45ms 间隔依次浮入，带一点回弹；只播第一屏，滚动不会重播。
- 桌面图标属于主题的一部分：六个字形配当前配色，可在设置里换。开屏用的是同一颗图标，底色跟随亮暗与当前配色，不会闪错色。

## 安装

到 [Releases](https://github.com/yuzlyn/DocBox/releases) 下载 APK，Android 8.0（API 26）以上可用。

APK 用的是 Android debug 证书签名，和 v1.0.0 起一直是同一张，可以直接覆盖旧版本安装，不用先卸载。这不是上架 Google Play 用的签名。

## 构建

需要 JDK 17 以上，以及装有 platform 37 和对应 build tools 的 Android SDK。

```bash
git clone https://github.com/yuzlyn/DocBox.git
cd DocBox
./gradlew assembleDebug
```

`./gradlew test` 跑 JVM 测试。`./gradlew connectedDebugAndroidTest` 跑仪器测试，其中一组会把 PNG／PDF 导出样张写进 App 的外部文件目录，方便逐张看。

`./gradlew assembleRelease` 出的包用 `app/build.gradle.kts` 里指定的 debug keystore 签名。要发布到别处，先换成自己的 keystore。

## 技术栈

| 分类 | 使用 |
|---|---|
| 语言 | [Kotlin](https://kotlinlang.org) 2.2.10 |
| 界面 | [Jetpack Compose](https://developer.android.com/develop/ui/compose)（BOM 2026.02.01）+ [Material 3 Expressive](https://m3.material.io) 1.5.0-alpha26 |
| 导航 | [Navigation 3](https://developer.android.com/guide/navigation) 1.2.1 |
| 架构 | MVVM，`ViewModel` + `StateFlow` |
| Markdown 渲染 | [markdown-it](https://github.com/markdown-it/markdown-it) 14 + [highlight.js](https://highlightjs.org) + [MathJax](https://www.mathjax.org)，打包进 WebView 预览引擎 |
| 文件访问 | [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)（DocumentFile）1.0.1 |
| 本地数据 | [Room](https://developer.android.com/training/data-storage/room) 2.8.5、[DataStore](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.7 |
| PDF 取文 | [PDFBox-Android](https://github.com/TomRoush/PdfBox-Android) 2.0.27.0 |
| 配色 | [material-kolor](https://github.com/jordond/materialkolor) 4.0.5 |
| 构建 | Gradle 9.4.1、AGP 9.2.1、KSP 2.2.10-2.0.2、version catalog |

## 目录结构

```
app/src/main/java/com/yuzlyn/mdeditor/
├── MainActivity.kt              # 入口、Navigation 3 回退栈、转场
├── data/
│   ├── local/                   # Room 数据库、DAO、实体、迁移
│   ├── repository/              # 笔记仓库与合并规则
│   ├── settings/                # DataStore 支撑的 AppSettings
│   ├── preview/                 # 预览调色与渲染配置
│   ├── profile/                 # 导出名片用的作者资料
│   ├── book/                    # EPUB／MOBI／FB2 解析与阅读状态
│   ├── importer/                # DOCX、PDF、HTML 读取
│   ├── export/                  # DOCX 写入、PDF／PNG 渲染、名片
│   ├── fonts/                   # 用户字体导入
│   ├── backgrounds/             # 笔记背景
│   └── stats/                   # 阅读与编辑统计
├── ui/
│   ├── component/               # 直排正文编辑器、字格、稿纸
│   ├── i18n/                    # 四语字符串与说明页内容
│   ├── navigation/              # 导航目标
│   ├── screen/                  # 编辑页、书房、设置、作者、说明
│   ├── reader/                  # 书籍阅读器
│   ├── stats/                   # 统计页与统计设置
│   ├── theme/                   # 色彩、字体、间距、启动图标
│   └── viewmodel/               # 各页面状态

app/src/test/                    # JVM 测试（解析、排版、导出、统计）
app/src/androidTest/             # 仪器测试与导出渲染
preview-engine/                  # WebView 预览引擎源码
icon-samples/                    # 启动图标生成脚本与样例
tools/                           # 辅助脚本（生成测试书籍）
```

## 数据与隐私

- 笔记正文是你选的那个文件夹里的 `.md` 文件，卸载 App 不会删掉它们。
- 标题、标签、排版、草稿、统计和导入的书存在 App 私有目录。
- App 没有声明联网权限，内容不会离开设备。

## 许可

[MIT](LICENSE) © 2025 Yuzlyn

---

<p align="center">
  <sub>作者 <a href="https://github.com/yuzlyn">Yuzlyn</a></sub>
</p>
