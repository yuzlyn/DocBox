# AGENTS.md — DocBox

> AI 动本仓库代码前必读。细则在 [docs/CONVENTIONS.md](docs/CONVENTIONS.md)。
> **本文件列出的每条命令都实际跑过**（2026-11-26 复验，JDK 17.0.19 / Gradle 9.4.1 / AGP 9.2.1）。

## 一句话描述

DocBox 是一个纯本地、无 INTERNET 权限的 Android Markdown 笔记 + 电子书阅读器：正文是用户自选文件夹里的 `.md`，Room 只存元数据，Kotlin 2.2 + Compose + Material 3 Expressive 构建。

## 命令（已实测）

| 命令 | 实测结果 |
|---|---|
| `./gradlew assembleDebug` | ✅ 增量约 20s → `app/build/outputs/apk/debug/app-debug.apk`（47.9 MB） |
| `./gradlew test` | ✅ 210 用例 / 27 个测试类 / 0 失败 |
| `./gradlew assembleRelease` | ✅ 1m33s（沿用 debug 签名，见细则） |
| `./gradlew lintDebug` | ✅ 0 error / 62 warning / 12 hint，构建成功 |
| `./gradlew connectedDebugAndroidTest` | ⚠️ 未验证通过：被设备安装授权阻断 |
| `python tools/make_test_books.py` | ⚠️ 能跑但**非幂等**，会改写已入库样本 |

**lint 现在是绿的**（2026-11-26 复验 0 error）。62 个 warning 不作门禁，但 **error 必须保持为 0**。
那 4 条曾经是「UnusedMaterial3ScaffoldPaddingParameter ×2 + JavascriptInterface ×2」，修复手法与踩到的 lint 解析坑见
[docs/CONVENTIONS.md](docs/CONVENTIONS.md) 第 9 节——别照着旧写法再写回去。

**环境约束**：

- `connectedDebugAndroidTest` 在 BRT-W09（Android 12）上报 `INSTALL_FAILED_ABORTED: User rejected permissions`，须在平板上手动点「允许」才能装上。
- 同时连多台设备时，任何 `adb` 命令都会报 `more than one device/emulator`；必须用 `ANDROID_SERIAL=<serial> adb ...` 指定目标。
- `tools/make_test_books.py` 每次运行都会因 zip 写入当前时间戳而改写 `app/src/test/resources/books/sample.epub`，跑完要 `git checkout --` 复原。

## 禁止做的事

1. **禁止给 `AppStrings` 加构造参数。** 已有 369 个字段，JVM 方法签名上限 255 个参数槽，加一个就 `ClassFormatError: Too many arguments in method signature`——编译能过，类加载时才炸。新字段一律照 `lateinit var` + 四份 `apply { }` 填值的现有写法加。
2. **禁止只改一种语言。** 界面文案必须同步 `ZhTwStrings` / `ZhCnStrings` / `EnUsStrings` / `FrFrStrings` 四份；`AppStringsFillTest` 会检查四语齐全且无空串。
3. **禁止在 `data/` 里写用户可见的句子。** 数据层只抛错误码（`BookError` / `ImportError` / `ImageInsertError`），文案在 `ui/i18n/LocalizedLabels.kt` 组；异常消息只放语言中立的细节（路径、序号），界面层再套括号拼接。
4. **禁止硬编码颜色值。** UI 取 `MaterialTheme.colorScheme.*`。现存 14 处字面量仅限 `ui/reader/`（位图色值转换）与 `SupportCodes.kt`（品牌色），不得新增。
5. **禁止新增 INTERNET 权限。** 清单里只有 `READ_EXTERNAL_STORAGE`（maxSdk 32），「数据不出设备」是产品承诺。
6. **禁止改了用户可见功能却不动说明页。** 必须同步 `ui/i18n/HelpContent.kt` 四份文案。
7. **禁止在没跑过的情况下往本文件加命令。** 猜的命令一律不进。
8. **禁止提交** `local.properties`、`build/`、`.gradle/`、`app/build/`。
9. **真机只在使用者明确要求时碰，而且只做「装」和「看」。** 未经允许不得
   `adb push` 测试文件、不得清 App 数据、不得读与本次改动无关的日志。使用者要求安装后，
   可以做 `install -r`、`screencap`、`uiautomator dump` 来核对自己改的界面；改过的系统
   设置（日夜模式等）必须还原，不得留下机器造的数据。最终功能仍由使用者验收；
   不涉及设备的部分（合并清单、编译产物、`aapt2 dump`）照常静态核对。
   **动画手感、点击反馈这类主观项一律交给使用者**：不要用 `input tap` / `input motionevent`
   反复模拟人手，也不要为了「看一眼」连续截图；模拟既得不到直观感受，又慢、又打断使用者。
   AI 只在必要时做一次可量化的静态测量（例如按下前后控件像素尺寸），之后如实说明未验项。
10. **禁止自造第二层开屏或拆分启动状态。** 开屏统一走 AndroidX SplashScreen：`installSplashScreen()` 必须在 `super.onCreate()` 前；保留条件只读轻量状态。设置值与 loaded 必须是同一份原子快照，禁止 `runBlocking`、独立收集两份启动状态或再盖一层空白 Compose 开屏。
11. **禁止按“是不是平板”写死布局。** 只能依据当前窗口/父容器约束适配；长文与表单优先复用 `ConstrainedContent`；瀑布流必须用 `LazyVerticalStaggeredGrid` + `masonryColumns()`（按内容区实际宽度算固定列数，360dp 手机必须仍是两列），禁止再用 `StaggeredGridCells.Adaptive(≥168dp)`——它在 336dp 可用宽下只给一列，会让瀑布流退化成列表、切换看似失效。所有页面必须检查底部 navigationBars、短横屏与大字号；长面板和对话框内容必须可滚动，但不得在 `LazyColumn` 外再套 `verticalScroll`。相邻的 32dp 图标按钮之间至少留 16dp：M3 最小触控区 48dp，间距更小时触控区会重叠、点 A 会打到 B。
12. **禁止破坏 M3 色彩与动效语义。** 不得全域交换或合并 `surfaceContainer*` 色阶；空间属性用 spatial spring，颜色/透明度用 effects spring，不新增 `tween` 或自写缓动。系统栏**图标明暗**必须跟随 App 实际亮暗模式，不能只跟系统 uiMode（只改明暗，不给系统栏刷色，见第 17 条）。
13. **新增目的地禁止留下空白分派。** 新 `NavKey` 必须同时接 `entryProvider`、真实入口和返回路径；`else -> NavEntry(key) { }` 不得用于掩盖漏接。当前主页、主页分段与笔记本过滤（`homeTarget` / `homeSection` / `notesTag`）必须用 `rememberSaveable`：Activity 重建后要停在原页，不能退回启动页设置。
14. **复杂逻辑必须写繁体中文“为什么”注释。** 状态竞态、窗口约束/insets、IO 与线程、动画规格、兼容性与分页算法都要解释原因和边界；只复述代码行为的注释不算达标。
15. **所有可用交互控件必须有且只有一套 M3 按压反馈。** 原生 Material 组件保留原生 state layer；自绘控件统一 `pressRipple`、共享同一个 `InteractionSource` 并按真实形状裁切。禁止嵌套双反馈、把整行裁成圆、禁用项仍播放动画，或用 `indication = null` 掩盖缺失反馈。缩放走 spatial spring、透明度走 effects spring，绘制读取的必须是 snapshot state。
16. **Nav3 的 entry 内容必须自己订阅状态。** `NavDisplay(entryProvider = …)` 会把每个 entry
   的内容 lambda 留住；`appSettings`、`notes`、`profile` 这类只被 lambda 捕获的普通值不会
   触发重组——设置写进 DataStore、界面却不刷新就是它。凡是后续会变的参数，entry 内容里
   一律读 `rememberUpdatedState(...).value`（见 `MainActivity` 的 `liveSettings` / `liveNotes`）。
   写设置必须传 transform（`onChange { current -> current.copy(...) }`），不得传组合时算好的
   `settings.copy(...)`；否则连点会用旧快照把结果原样写回。
17. **系统栏必须保持透明，不得用不透明 scrim「对齐颜色」。** 只允许用
   `WindowInsetsControllerCompat` 控制 `isAppearanceLightStatusBars/NavigationBars`；
   `enableEdgeToEdge` 保持默认透明。透明之后，每个页面必须自己消费 insets
   （M3 `TopAppBar` 的 `windowInsets`、自定义栏的 `statusBarsPadding()`、底部的
   `navigationBarsPadding()`），否则内容会跑到时间、电量和挖孔下面。
18. **移除功能按层清，加功能先问要不要。** 移除一个功能必须同时清掉：页面、数据层、
   路由与 `entryProvider`、抽屉/设置/统计入口、ViewModel 字段与预加载、Settings 字段与
   DataStore key、枚举值、四语 `AppStrings`（声明 + 四份赋值）、`HelpContent`、文档、
   脚本、测试与资源；并用繁简两种关键词全仓 grep 验收。反过来，没有明确要求不要自造
   整套模块（前一版凭空加了「小學」：16 个数据文件、640 条四语文案、路由、抽屉、统计卡，
   最后整块移除）。

## 完成标准

以下全部满足才算改完：

1. `./gradlew assembleDebug` 通过。
2. `./gradlew test` 通过，用例数不低于基线 **210**。
3. 动过文案 → 四语齐全，`AppStringsFillTest` 绿。
4. 动过用户可见功能 → `HelpContent.kt` 四份同步更新。
5. `./gradlew lintDebug` 通过，**error 必须为 0**（warning 不设限）。
6. 无新增硬编码颜色、无新增数据层中文句子。
7. `git diff` 中不出现 `local.properties`、`build/`、测试样本二进制。
8. commit message 说明「为什么改」，而不是「改了什么」。
9. 真机上的行为由使用者验收（见「禁止做的事」第 9 条），交付时如实说明哪一段没验过。
10. 动过 UI、导航、设置或删过功能 → 对照 `docs/CONVENTIONS.md` 第 13 节的回归清单逐项自查，
    并在交付说明里写出「哪些坑已确认不会复发」。

## 目录速查

| 路径 | 职责 |
|---|---|
| `app/src/main/java/com/yuzlyn/mdeditor/data/` | 纯数据层（可 JVM 单测）：Room、仓储、导入器、导出、统计 |
| `app/src/main/java/com/yuzlyn/mdeditor/ui/` | Compose 界面：screen、reader、stats、component、theme |
| `app/src/main/java/com/yuzlyn/mdeditor/ui/i18n/` | 四语文字表、帮助内容、错误码文案 |
| `app/src/test/` | JVM 测试（解析/排版/导出/统计/四语文案） |
| `app/src/androidTest/` | 仪器测试与导出渲染（写 PDF/PNG 供人眼核对） |
| `preview-engine/` | WebView 预览引擎源码（markdown-it + highlight.js + MathJax） |
| `tools/` | 辅助脚本（测试电子书生成） |

## 细则

M3 视觉规范、架构分层、i18n 工作流、导入器、签名策略、已知技术债 → [docs/CONVENTIONS.md](docs/CONVENTIONS.md)
