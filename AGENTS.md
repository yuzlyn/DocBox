# AGENTS.md — DocBox

> AI 动本仓库代码前必读。细则在 [docs/CONVENTIONS.md](docs/CONVENTIONS.md)。
> **本文件列出的每条命令都实际跑过**（2026-11-16，JDK 17.0.19 / Gradle 9.4.1 / AGP 9.2.1）。

## 一句话描述

DocBox 是一个纯本地、无 INTERNET 权限的 Android Markdown 笔记 + 电子书阅读器：正文是用户自选文件夹里的 `.md`，Room 只存元数据，Kotlin 2.2 + Compose + Material 3 Expressive 构建。

## 命令（已实测）

| 命令 | 实测结果 |
|---|---|
| `./gradlew assembleDebug` | ✅ 33s → `app/build/outputs/apk/debug/app-debug.apk`（47.9 MB） |
| `./gradlew test` | ✅ 188 用例 / 25 个测试类 / 0 失败 |
| `./gradlew assembleRelease` | ✅ 1m33s（沿用 debug 签名，见细则） |
| `./gradlew lintDebug` | ✅ 0 error / 60 warning，构建成功 |
| `./gradlew connectedDebugAndroidTest` | ⚠️ 未验证通过：被设备安装授权阻断 |
| `python tools/make_test_books.py` | ⚠️ 能跑但**非幂等**，会改写已入库样本 |

**lint 现在是绿的**（2026-11-16 修掉了最后 4 个 error）。60 个 warning 不作门禁，但 **error 必须保持为 0**。
那 4 条曾经是「UnusedMaterial3ScaffoldPaddingParameter ×2 + JavascriptInterface ×2」，修复手法与踩到的 lint 解析坑见
[docs/CONVENTIONS.md](docs/CONVENTIONS.md) 第 9 节——别照着旧写法再写回去。

**环境约束**：

- `connectedDebugAndroidTest` 在 BRT-W09（Android 12）上报 `INSTALL_FAILED_ABORTED: User rejected permissions`，须在平板上手动点「允许」才能装上。
- 同时连多台设备时，任何 `adb` 命令都会报 `more than one device/emulator`；必须用 `ANDROID_SERIAL=<serial> adb ...` 指定目标。
- `tools/make_test_books.py` 每次运行都会因 zip 写入当前时间戳而改写 `app/src/test/resources/books/sample.epub`，跑完要 `git checkout --` 复原。

## 禁止做的事

1. **禁止给 `AppStrings` 加构造参数。** 已有 359 个字段，JVM 方法签名上限 255 个参数槽，加一个就 `ClassFormatError: Too many arguments in method signature`——编译能过，类加载时才炸。新字段一律照 `lateinit var` + 四份 `apply { }` 填值的现有写法加。
2. **禁止只改一种语言。** 界面文案必须同步 `ZhTwStrings` / `ZhCnStrings` / `EnUsStrings` / `FrFrStrings` 四份；`AppStringsFillTest` 会检查四语齐全且无空串。
3. **禁止在 `data/` 里写用户可见的句子。** 数据层只抛错误码（`BookError` / `ImportError` / `ImageInsertError`），文案在 `ui/i18n/LocalizedLabels.kt` 组；异常消息只放语言中立的细节（路径、序号），界面层再套括号拼接。
4. **禁止硬编码颜色值。** UI 取 `MaterialTheme.colorScheme.*`。现存 14 处字面量仅限 `ui/reader/`（位图色值转换）与 `SupportCodes.kt`（品牌色），不得新增。
5. **禁止新增 INTERNET 权限。** 清单里只有 `READ_EXTERNAL_STORAGE`（maxSdk 32），「数据不出设备」是产品承诺。
6. **禁止改了用户可见功能却不动说明页。** 必须同步 `ui/i18n/HelpContent.kt` 四份文案。
7. **禁止在没跑过的情况下往本文件加命令。** 猜的命令一律不进。
8. **禁止提交** `local.properties`、`build/`、`.gradle/`、`app/build/`。

## 完成标准

以下全部满足才算改完：

1. `./gradlew assembleDebug` 通过。
2. `./gradlew test` 通过，用例数不低于基线 **188**。
3. 动过文案 → 四语齐全，`AppStringsFillTest` 绿。
4. 动过用户可见功能 → `HelpContent.kt` 四份同步更新。
5. `./gradlew lintDebug` 通过，**error 必须为 0**（warning 不设限）。
6. 无新增硬编码颜色、无新增数据层中文句子。
7. `git diff` 中不出现 `local.properties`、`build/`、测试样本二进制。
8. commit message 说明「为什么改」，而不是「改了什么」。

## 目录速查

| 路径 | 职责 |
|---|---|
| `app/src/main/java/com/yuzlyn/mdeditor/data/` | 纯数据层（可 JVM 单测）：Room、仓储、导入器、导出、统计 |
| `app/src/main/java/com/yuzlyn/mdeditor/ui/` | Compose 界面：screen、reader、stats、component、theme |
| `app/src/main/java/com/yuzlyn/mdeditor/ui/i18n/` | 四语文字表、帮助内容、错误码文案 |
| `app/src/test/` | JVM 测试（188 用例，解析/排版/导出/统计） |
| `app/src/androidTest/` | 仪器测试与导出渲染（写 PDF/PNG 供人眼核对） |
| `preview-engine/` | WebView 预览引擎源码（markdown-it + highlight.js + MathJax） |
| `tools/` | 辅助脚本（测试电子书生成） |

## 细则

M3 视觉规范、架构分层、i18n 工作流、导入器、签名策略、已知技术债 → [docs/CONVENTIONS.md](docs/CONVENTIONS.md)
