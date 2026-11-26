# DocBox 项目细则

> 本文件承载 [AGENTS.md](../AGENTS.md) 移出的细节。根文件只留硬规则与门禁，避免每次 AI 会话都吞掉大段背景。
> 最后核对：2026-11-26（新增第 13 节「回归清单」）。

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
2. **间距**：一律用 `ui/theme/Spacing.kt` 的 `Space` token，不写字面量。刻度是 `spaceN = N/100 × 8dp`：
   `s0(0) s25(2) s50(4) s75(6) s100(8) s125(10) s150(12) s175(14) s200(16) s250(20) s300(24) s400(32) s450(36) s500(40) s600(48) s700(56) s800(64) s900(72)`。
   非 8 倍数的 2/4/6/10/14 只用于真实组件需要的内嵌尺寸；padding 定义在父容器上，子元素不写 margin。
   字号放大到 200% 时**不缩小间距**。
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

- `app/src/test/` — JVM 单测，208 用例 / 27 类。覆盖解析器、排版、导出、统计、i18n 完整性。
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
| `lintDebug` | ✅ 0 error（2026-11-26 复验） | 仍余 62 warning / 12 hint，不作门禁 |
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

## 11. 启动、主题与系统栏

### 11.1 SplashScreen 只有一条启动链路

- 依赖集中在 `gradle/libs.versions.toml`：`androidx.core:core-splashscreen:1.2.0`；不要在模块里另写版本号。
- `MainActivity.onCreate` 第一行是 `installSplashScreen()`，早于 `super.onCreate()` 和任何根 View 操作。
- `Theme.MDEditor` 是 Splash 主题，父类 `Theme.SplashScreen.IconBackground`；`postSplashScreenTheme` 指向 `Theme.MDEditor.Base`。兼容属性不要只写 `android:` 版本或只放 `values-v31`，否则 API 26–30 失效。
- `setKeepOnScreenCondition` 只读 `FileViewModel.settingsState.value.loaded`。不得在这里读磁盘、调用 `settings.first()` 或 `runBlocking`。
- `AppSettingsState(settings, loaded)` 是唯一的启动原子快照。ViewModel 负责 1500ms 超时兜底；Activity、首页和主题不得再各自收集一份“真实设置”或另设 loaded。首页只在首次 Ready 时捕获 `homeTarget`，之后修改默认首页不应把当前页面突然切走。
- 退出用 `setOnExitAnimationListener`；结束时必须 `provider.remove()`。`windowSplashScreenAnimationDuration` 只描述 AVD 图标动画，不控制开屏保留时间；没有 AVD 就不要填。

### 11.2 Monet 与亮暗资源

- API 31+：`values-v31/colors.xml`、`values-night-v31/colors.xml` 引用 `system_neutral1_*` / `system_accent1_*`，跟随系统壁纸；低版本用 `values/`、`values-night/` 的静态回落。
- 系统起始窗只能先知道系统 uiMode，无法读取 DataStore 里的强制亮暗。系统 Splash 保留到真实主题 Ready 后，退出时再把背景对齐 Compose 的实际 `surface`；不得宣称 XML 能消除所有强制模式冷启动色差。
- `Theme.kt` 保留 Monet/MaterialKolor 生成的完整 `ColorScheme`，不得把多个 `surfaceContainer*` 全局改成同一个颜色。背景、卡片、菜单、对话框各自选语义正确的 token。
- **系统栏保持透明**：`enableEdgeToEdge()` 只在 `onCreate` 调一次，沿用默认的透明样式；`Theme.kt` 只按 App 实际 `darkTheme` 设 `isAppearanceLightStatusBars / isAppearanceLightNavigationBars`，**不得**再用 `SystemBarStyle.dark(surfaceColor)` 之类的实色 scrim。那会把状态栏刷成不透明色、盖住应用画到顶部的区域：编辑器顶栏是 `surfaceContainer`，于是顶部留下一条更黑的 `surface`，就是「状态栏割裂」。详见 13.3。

## 12. 自适应 UI、导航与注释

### 12.1 布局判断与版心

- 应用级布局看当前 App 窗口，组件级布局看父容器约束；不得用物理设备型号或“平板”布尔值。分屏、折叠屏、桌面窗口与 200% 字级都属于同一套适配门槛。
- 长文、设置、表单统一复用 `ui/component/ConstrainedContent.kt`：显式取 `min(父容器可用宽度, 上限)`，不要依赖 `widthIn` / `fillMaxSize` 的顺序猜测。
- 现行版心：设置/帮助约 720dp，个人资料 560dp，Reader 与横排 Editor 840dp，PDF 960dp，Stats 840dp；Author 520dp、Support 520dp 与 Markdown 预览 720px 已独立实现，不重复包裹。
- Main/Library 的瀑布流一律用官方的 `LazyVerticalStaggeredGrid`，列数由 `ui/component/MasonryColumns.kt` 的 `masonryColumns(可用宽度)` 计算，再交给 `StaggeredGridCells.Fixed`。
  **不要用 `StaggeredGridCells.Adaptive(...)`**：360dp 手机扣掉左右 12dp 只剩 336dp，`Adaptive(180.dp)`（甚至 168dp）都只算出一列，瀑布流会退化成单列，和 LIST 长得一样——排版切换因此看起来「完全没反应」。`masonryColumns` 的 168dp 基准正是为了让 336dp 刚好两列。
  可用宽度取「内容区实际约束」（`BoxWithConstraints.maxWidth`，平板要再扣掉 rail 与左右内边距），不要拿整窗宽度硬除。LIST 单列才额外限宽，瀑布流不要限宽。
  `BoxWithConstraints` 只用于**内容级**尺寸（网格列数、版心）；判断 rail / `expanded` 仍用 `LocalConfiguration.screenWidthDp`——前者第一帧可能量到 0，会让侧栏从 0 宽展开闪一下。
- expanded rail 的现行断点是窗口宽度 840dp；高度低于 480dp 时 rail 可滚动。短横屏滚动容器里不得再用 `weight` 无限撑高。

### 12.2 Insets、滚动与导航验收

- 浮层 Scaffold 忽略 `innerPadding` 的页面，必须自己把 `WindowInsets.navigationBars` 的底部值并入内容 padding；笔记列表还要预留 FAB 空间。不要把静态 `innerPadding` 机械套回浮层顶栏。
- BottomSheet 的长内容、AlertDialog 的可增长内容必须 `verticalScroll`。`LazyColumn`、Lazy 网格自身就是滚动容器，不得再包一层 `verticalScroll`。
- 新 `NavKey` 的验收顺序：声明 → `entryProvider` 分派 → 可见入口 → 参数/ViewModel → 返回路径。不能只声明 key 或只 import Screen；空白 `else` 不是兜底功能。
- Compose 动效统一 `MaterialTheme.motionScheme`：空间属性用 spatial，颜色/透明度用 effects。不得新增 `tween`、硬编码 easing。`drawWithContent`/Canvas 的动画值要读明确的 snapshot state；若直接读 `Animatable.value` 不能触发重绘，须同步到 `mutableFloatState` 后再画。

### 12.3 注释要求

代码注释使用繁体中文，至少解释下列“为什么”，不要只把表达式复述一次：

- **状态**：快照来源、初始化时序、为何不会重复发射或发生竞态。
- **布局**：为什么取这个约束、insets 由谁消费、为什么这里需要限宽/滚动。
- **线程与资源**：为什么切 IO、谁负责关闭资源、超时/失败怎样回落。
- **动效**：为什么属于 spatial 或 effects，结束回调如何释放。
- **兼容性**：API 守卫、低版本行为、平台限制；无法静态验证的部分要写明，不能假装实测。

簡單 getter、显而易见的参数名不必堆注释；复杂注释必须指出原因、边界与后续修改不能破坏的条件。

### 12.4 点击反馈验收

- **原生不叠加**：`Button`、`IconButton`、`TextButton`、FAB、FilterChip、Switch、NavigationDrawerItem 等原生组件已有 M3 state layer，不另加 `pressRipple`。
- **自绘统一**：`pressRipple` 负责 2% 回缩与形状波纹；同一交互节点共用同一个 `InteractionSource`，`clickable` / `selectable` 的 `indication = null` 只能用于避免双反馈，不能用来关闭唯一反馈。
- **形状一致**：整行动作不能用 CircleShape；按实际外形容器选 `RoundedCornerShape` / CircleShape。嵌套容器只让一层播放动画，例如二维码只在图像容器反馈，外层保留整卡热区。
- **状态一致**：缩放使用 fast spatial spring，透明度使用 effects spring。半径、透明度、缩放和按下位置都要通过 snapshot state 驱动重绘，不能只在普通变量中保存。
- **无语义冲突**：`checked` 只表示用户选择，`enabled` 只表示可操作性；禁用与当前选中的空操作不得播放反馈。真正禁用才降透明度，当前选中的单选项可关闭交互但保持正常选中色。
- **避免重复**：整行可点设置项里的 Switch/RadioButton 只显示状态（`onCheckedChange = null` / `onClick = null`），由整列统一切换与反馈；不要在父行和子控件各做一次动画。
- **卡片与按钮是两套比例**：卡片（笔记／笔记本／书籍）统一走
  `pressRipple(pressedAlpha = CARD_RIPPLE_ALPHA /* 0.12 */, pressedScale = CARD_PRESSED_SCALE /* 0.94 */, minPressHoldMs = MIN_PRESS_HOLD_MS /* 140 */)`。
  卡片面积大，0.94 相当于十几 dp 的位移；但同样必须套最短保持——0.96 且抬手就回时，
  正常点按只看得到波纹、看不到缩放。三者必须用同一个 modifier，不要一半用原生
  `Surface(onClick)`、一半用自绘波纹，否则圆角裁切与时序会不一致。
  卡片**不用** `pressScale` 再叠一层：`pressRipple` 已经负责缩放，重复会变成两套弹簧。
- **卡片内距四边一致**：笔记卡、笔记本卡、书籍卡都用 `Modifier.padding(Space.s200)` + `Arrangement.spacedBy(Space.s100)`。
  不许只写 `start/top/end`——漏掉 `bottom` 时最后一行元数据（`EPUB · 24.9 MB…`）会直接贴在卡片下缘，
  标题一换行就更像「正文和下面的资讯黏在一起」。改卡片内容时也不要顺手加 `padding(bottom = …)`，
  四边同一个值才和另外两类卡对齐。
- **侧栏的状态变化用 effects spring**：`RailButton` 的选中容器色与图标色用 `animateColorAsState(defaultEffectsSpec())`，换页时不跳色；排版钮的图标用 `AnimatedContent` 交叉淡入淡出（出场 fast effects、入场 default effects + spatial），不要瞬间换图。笔记页与书库页的侧栏底色必须是同一个值（`onSurface @5%`），否则切页时整条侧栏会突然变深。
- **按压缩放要看得出来，而且要留够时间**：顶栏/侧栏圆形按钮与头像统一用 `PRESSED_SCALE = 0.88` + `fastSpatialSpec()`（M3E 的 damping 0.6 / stiffness 800）。32dp 的顶栏钮缩 3.8dp，48dp 侧栏钮缩 5.8dp；**不要低于 0.94**——0.96 在 32dp 上只有 1.3dp，肉眼等于没动。
  更关键的是**最短保持 140ms**：`fastSpatialSpec` 要约 70ms 才压到 0.88，而正常点按的「按下→抬起」只有 50–80ms，按弹簧直接反向的话只有长按看得到、普通点按等于没动。写法见 `ui/component/PressScale.kt`：按下用 `Animatable` 压下去，抬手先 `delay` 到满 140ms 再回弹；连按时要取消上一次排队中的回弹。
  缩放只作用于视觉层（`graphicsLayer`），布局与 48dp 触控区不变；头像的 `pressRipple` 关掉自带缩放（`animateScale = false`），统一交给 `pressScale`。
- **触控与无障碍**：视觉 24dp 的删除图标仍需至少 48dp 触控区域，可用 Material3 `minimumInteractiveComponentSize`；不能只给图标补热区而漏掉 contentDescription。

### 12.5 抽屉（模态侧栏）的动效与性能

- **开关状态用 `State` 传进抽屉，由抽屉自己读**。`MainActivity` 若写 `open = drawerOpen`（读到值），
  每次开关都会重算整个主内容，`NavDisplay` 的 `entryProvider` 也跟着重建；抽屉动画的第一帧
  就在和整页重组抢时间，表现是「一开就卡一下」。正确写法：`val drawerOpen = remember { mutableStateOf(false) }`
  + `HomeDrawer(open = drawerOpen)`，抽屉内部 `val isOpen = open.value`。
- **位移与遮罩分两条轨道**：抽屉用 `defaultSpatialSpec`（partial-screen），遮罩用 effects；
  进场 `defaultEffectsSpec`、退场 `fastEffectsSpec`——先清场再进场的序列化节奏。不要给遮罩套 spatial。
- **位移只走 `graphicsLayer.translationX`**，不要动画 `offset`/`width`/`padding`；后者每帧 relayout。
- **遮罩是 `scrim` 角色 @32%**；抽屉容器 `surfaceContainerLow`，只圆靠内容那一侧（16dp）。
  本项目零静态阴影，层级由遮罩 + 容器色承担，抽屉不加 elevation。
- 弹簧在贴边处会过冲，必须 `coerceIn(0f, 1f)`，否则左缘会露缝；代价是落位那一下略硬，
  这是 expressive spatial 在边锚定表面上的固有取舍。
- 现存差距（未改，属交互而非动效）：拖动关闭目前只累计位移、松手才判定，抽屉不跟手；
  MD3 的抽屉应当跟手并在松手时按速度/距离落位。

### 12.6 多选顶栏与列表删除动画

- 筆記頁與書庫頁共用 `ui/component/HomeChrome.kt` 的 `SelectionTopBar`：關閉、`selectedCount`、
  可選的 `secondaryActions` 插槽、刪除（error 色）。不要各寫一份，兩頁的列高與圖示順序會漂。
- 選取狀態用 `Set<String>`；長按加入、選取中點卡片切換、返回鍵清空。兩頁一致。
- **平板（expanded）也必須顯示多選頂欄**：筆記頁的頂欄原本包在 `visible = !expanded` 裡，
  長按選中後沒有任何刪除入口；條件要寫成 `!expanded || selectedIds.isNotEmpty()`。
- 刪除動畫由清單狀態驅動：`Modifier.animateItem(fadeIn = defaultEffects, placement = defaultSpatial,
  fadeOut = fastEffects)`。三類卡片與書卡同一組參數；不要自己寫 `AnimatedVisibility` 包卡片，
  否則位置動畫與淡出會各跑一套。
- **卡片上不得放單本刪除鈕**：刪除一律走長按多選的頂欄，兩頁一致。曾經書庫卡片右下角有一顆
  48dp 刪除鈕，多選時用 `if (!selectionMode)` 隱藏——結果長按的瞬間每張卡少掉一行 48dp，
  瀑布流整片重排，看起來像「卡片變形」。卡片的內容量與高度必須與選取狀態無關，
  選中只換容器色（`animateColorAsState`），不要改任何會影響尺寸的分支。
- 多筆刪檔切 IO 再 `reload()`：主執行緒逐檔刪除會讓清單在動畫開始前先卡住。
- **分享原檔不要複製**：書庫的分享走 `data/book/BookShare.kt`，`FileProvider` 直接授權
  `filesDir/books` 裡的檔案（`res/xml/file_paths.xml` 必須有
  `<files-path name="books" path="books/"/>`）。一本書幾十 MB，先寫一份到 cache 既慢又佔空間。
  多選用 `ACTION_SEND_MULTIPLE` + `*/*`；單選給精確型別。`ClipData` 要與 `EXTRA_STREAM`
  一起寫，否則部分接收端拿不到讀取權限。
- 分享按鈕放 `secondaryActions` 插槽、在刪除左邊，位置與筆記頁一致；失敗用
  `msgNoShareTarget` / `msgShareFailed` 顯示在 snackbar，不要在資料層寫中文句子。

### 12.7 开屏：系统起始视窗 + App 自绘

开屏分成两段，职责不能混：

1. **系统起始视窗**（Android 12+ 那片底色＋图标，进程还没起来）只能用**编译期资源**，
   改不到运行时状态。所以：
   - 底色用 `values/colors.xml`（#F7F9F8）与 `values-night/colors.xml`（#101413）的静态值。
     **不要**改回 `@android:color/system_neutral1_*`：2026-11-26 实测它在深色模式下解析成近白
     （真机截图 (252,252,255)），比静态值更不可控；M3 的 surface 在任何取色下都是同一条中性色阶，
     静态值已经够接近。
   - **不要**指定 `windowSplashScreenAnimatedIcon`。留空时系统会取「启动的那条 activity-alias」
     的图标，也就是使用者选的字形＋那套配色，和桌面图标完全一致；写死某一份 drawable 会永远
     停在默认字形上（`@drawable/ic_launcher_foreground` 就踩过这个坑）。
   - 不要用 `setKeepOnScreenCondition { !settingsLoaded }` 把系统视窗扣住等设定：
     它只有静态色与静态图，扣得越久，看到「和 App 不一样的开屏」的时间越长。

2. **App 自绘的开屏**（`ui/component/SplashOverlay.kt`）负责真正对得上设定：
   - 底色取 `MaterialTheme.colorScheme.surface`（莫奈或种子色都成立）；第一帧 DataStore 还没读回来，
     先用 `data/settings/SplashCache.kt` 的上一份快照，读完再换即时值。
   - 标志直接用 `LauncherIcon.currentIconDrawable(context)` 点阵化——就是桌面上那颗别名图标，
     字形与配色都不会跳；尺寸固定 160dp（实测系统起始视窗画的就是 160dp，交棒那一帧不会缩放）。
   - 退场 240ms：整层淡出 + 标志放大 8%，期间吞掉触控；最短显示 300ms，从**第一帧**起算
     （从 `onCreate` 起算的话，冷启动已经花掉几百毫秒，App 这一段往往只剩一瞬）。
   - `playSplash` 只在 `savedInstanceState == null` 时为真：转屏、分屏、改日夜重建时不要再闪一次。

### 12.8 列表卡片的入场动效与标题行内徽章

**入场动效统一走 `ui/component/Appear.kt`**，和设置页、说明页是同一支：
45ms 步进、24dp 上浮、`spring(dampingRatio = 0.72, stiffness = MediumLow)`。
卡片页（全部笔记／笔记本／书库）只是把它挂到 Lazy 列表上，参数不许各写一套。

- **只播第一屏**。延迟列表是懒加载的，卷进来的项如果也重播，每滑一段就浮一次，
  比没有动效更糟。写法：页面级 `entrance` 布尔 + `enabled = entrance && index < APPEAR_ITEM_LIMIT`，
  900ms 后（`APPEAR_WINDOW_MS`）才组合出来的项直接显示。`enabled` 只在第一次组合时读取。
- **步进要封顶**。`APPEAR_MAX_STEPS = 10`：50 张卡也不会等到两秒多才全部出现。
- **`animateItem` 挂在 `Appear` 的 `modifier` 上**，不要再留在卡片里——否则一层套一层，
  位置动画和入场动画各跑一套。
- 状态要声明在 `LazyVerticalStaggeredGrid` **外面**：items 的 lambda 是 DSL，不是 @Composable，
  在里面写 `remember` / `LaunchedEffect` 编译不过。

**标题行的格式徽章（Word／PDF／EPUB…）**用 `AnnotatedString` 的行内内容，不要用 Row 排：

- `appendInlineContent(key, label)` + `InlineTextContent`，徽章会跟在标题最后一个字后面；
  标题换行时自然落在最后一行，正好是「贴最后一行最右侧」。
- 占位尺寸用 **sp**（60 × 22），字级放大时跟着放大；用 dp 的话 200% 字级会把标签裁掉。
- 用 Row + `Alignment.Bottom` 的写法会把徽章宽度从**每一行**里扣掉，168dp 的瀑布流卡片上
  标题会被挤成三行，所以不采用。
- 截断风险靠行数缓解：瀑布流 `maxLines = 3`、列表 `maxLines = 2`；再长就只能丢徽章。

**所有网格都必须读同一份排版设置**。顶栏／侧栏的「瀑布流↔列表」切换只写 `noteLayout`，
每个网格自己决定列数——写死成 `Fixed(masonryColumns(...))` 的页面，按钮点了只会改设置、
画面不动，看起来就是「按钮失效」（笔记本页踩过）。新增列表页时先确认这一条。
**列表模式不能只有标题**：至少给一行摘要（`bodySmall`，1 行）与时间（`labelSmall`），
和瀑布流共用同一组字级与颜色，只是行数更少。

## 13. 回归清单：2026-11-26 踩过的坑

这一节是实战记录，不是背景介绍。**每次改 UI、导航、设置或删除功能前，先对一遍。**

### 13.1 瀑布流列数是算出来的，不是估出来的

**症状**：Masonry 和 LIST 长得一模一样，切换像完全失效。

**根因**：`StaggeredGridCells.Adaptive(180.dp)` 在 360dp 手机（扣掉左右各 12dp，只剩 336dp）
只算出 1 列。`Adaptive` 的公式是 `(available + spacing) / (minSize + spacing)`，
180dp 时是 `(336+8)/(180+8)=1.83 → 1`。LIST 本来就是 `Fixed(1)`，两者像素级相同。

**规则**：官方 `LazyVerticalStaggeredGrid` + `ui/component/MasonryColumns.kt` 的
`masonryColumns(可用宽度)`，列数取内容区**真实约束**（`BoxWithConstraints.maxWidth`，
平板再扣 rail 与内边距）。基准 168dp：336dp 正好两列；低于 320dp 退回单列；上限 6 列。

**验收**：`MasonryColumnsTest` 已钉住 336dp→2、280dp→1、387dp→2、576dp→3、816dp→4、1600dp→6。
不要相信「180dp 应该能放两列」这类心算——算一遍 `Adaptive` 的公式。

### 13.2 Nav3 的 entry 内容会缓存，外层状态不会自动传进去

**症状**：设置明明写进 DataStore 了，界面不刷新；排版切换只改值不换视图；重启或重进页面才生效。

**根因**：`NavDisplay(entryProvider = { key -> NavEntry(key) { … } })` 会把 entry 的内容
lambda 留住。lambda 里直接用的 `appSettings`、`notes`、`profile` 只是**普通值**，没有
State 订阅，所以外层重组不会让 entry 重组。

**规则**：entry 内容的第一行读 `rememberUpdatedState(...).value`：

```kotlin
// MainActivity，NavDisplay 外层
val liveSettings = rememberUpdatedState(appSettings)
val liveNotes = rememberUpdatedState(notes)

// 每个 NavEntry 的内容里
val appSettings = liveSettings.value   // 这一行建立订阅
val notes = liveNotes.value
```

凡是「从 Flow 派生、以后会变」的参数都要这样处理。单纯改主题（在 NavDisplay 外层）看不出
问题，所以这个坑只有在设置页、排版、列表数据上才暴露。

entry 里用**同名 `val` 遮蔽外层变量**（`val appSettings = liveSettings.value`）是刻意的：
这样 entry 内部不用逐个改名，改动面最小。编译器只会提示 shadow，不要为了「消警告」把它
改回直接读外层。

**验收**：改设置后**不重启、不重进页面**，界面必须立即变；同时看 DataStore 文件与界面，
两者不一致就是这个问题。

### 13.3 系统栏必须透明，不能拿不透明 scrim「对齐颜色」

**症状**：状态栏一条 `surface`、紧挨着的顶栏一条 `surfaceContainer`，像被切了一刀；
或者内容永远画不到屏幕顶部。

**根因**：`enableEdgeToEdge(SystemBarStyle.dark(surfaceColor))` 把状态栏漆成**不透明**的
surface 色，盖住了应用自己画到 y=0 的顶栏。编辑器顶栏是 `surfaceContainer`，于是露出上半截
更黑的条。

**规则**：`enableEdgeToEdge()` 只在 `onCreate` 调一次，保持默认透明；`Theme.kt` 只用
`WindowInsetsControllerCompat` 设 `isAppearanceLightStatusBars / isAppearanceLightNavigationBars`。
不要在 Theme 里重设系统栏颜色。

**诊断手法**：临时把某个顶栏的 `containerColor` 改成 `Color.Red`，截图采样。红色必须从
`y=0` 开始；只到状态栏下缘就说明系统栏还是不透明。**验证完立刻还原**（见 13.7）。

### 13.4 Edge-to-edge 是全局契约，不是某个页面的事

系统栏透明之后，所有页面都要自己消费 insets：

- M3 `TopAppBar`：`windowInsets = WindowInsets.statusBars`（默认值已是，别传 `WindowInsets(0)`）。
- 自定义顶栏：`statusBarsPadding()`；侧栏还要 `navigationBarsPadding()`。
- 阅读器 WebView：`windowInsetsPadding(WindowInsets.statusBars)` + `navigationBars`。
- 列表底部：`navigationBarsPadding()` 或把导航栏高度并入 `contentPadding`；笔记列表再预留 FAB。

**验收**：截图采样顶部 0–120px，正文/卡片不得压在时间、电量和挖孔下面；亮暗两种模式都要看。

### 13.5 关掉系统文本工具栏，就要自己补齐剪贴板动作

**症状**：直排编辑器不能粘贴、不能剪切。

**根因**：用 `CompositionLocalProvider(LocalTextToolbar provides SilentTextToolbar)` 关掉了
系统工具栏，自绘工具列却只给了「复制 / 全选 / 取消」。

**规则**：自绘工具列至少要有复制、剪切、粘贴、全选、取消。粘贴先读 `clipboard.getText()`；
剪切先写剪贴簿再删文本（顺序反了只剩空串）。工具列用 `horizontalScroll` 包一层，
避免窄屏把最后一颗按钮切掉。

**验收**：选中文字后五个动作都能改变文本或剪贴板；贴上的内容与剪下的内容一致。

### 13.6 移除一个功能要按层清

**案例**：小學模块有 2 个页面、16 个数据文件、640 条四语文案、2 个路由、抽屉入口、
统计卡、ViewModel 字段与 21MB 预加载、2 个 Settings 字段、1 个枚举值、3 个生成脚本。

**清单**（缺一层就会留下死代码或空入口）：

1. 页面文件与专用组件
2. `data/` 子目录与资源/资产
3. `AppDestinations` 的 NavKey
4. `MainActivity` 的 `entryProvider` 分派与 `when`
5. 抽屉、设置页、统计页等可见入口
6. ViewModel 的字段、Flow、预加载与公开方法
7. `AppSettings` 字段 + `AppSettingsPreferences` 的 key / 读 / 写
8. 相关枚举值（如 `StatKind`）
9. 四语 `AppStrings`：声明 + 四份赋值（用脚本删，删完编译 + `AppStringsFillTest`）
10. `HelpContent` 四份
11. 文档、脚本、测试、资源

**验收**：用繁简两种关键词全仓 grep（`小學|小学`、`Study|study`、`prosody`…），
确认只剩历史记录；抽屉与统计页没有空入口；`AppStringsFillTest` 绿。

### 13.7 验证要能量化，临时诊断必须还原

- **列数**：截图数卡片列数，或直接算可用宽度。
- **颜色/边界**：用 `python + PIL` 采样 PNG 像素，不要用肉眼判断「差不多」。
- **控件坐标**：`adb shell uiautomator dump /sdcard/ui.xml` 拿 `bounds`，不要猜。
- **设置是否生效**：`run-as <pkg> cat files/datastore/app_settings.preferences_pb | strings`，
  与界面状态对照。
- **临时诊断**（红色顶栏、`Log.i("MDLayout", …)`、临时 `SystemBarStyle`）用完必须全部删除。
  交付前 grep 一遍：`grep -rn "Color.Red\|MDLayout\|TODO" app/src/main/java`。

### 13.8 顶栏小按钮与当前页：三个连在一起的坑

**症状**：全部笔记页的「瀑布流/列表」切换有时没反应，过一会儿发现书库的排版变了；
从某些状态回来后再点又正常。

1. **写入设置必须传 transform，不能传组合时算好的整份设置。**
   `onSettingsChange(settings.copy(noteLayout = …))` 里的 `settings` 是组合快照；DataStore
   还没回流时再点一次，仍按旧值算、把同一个值写回去。正确写法是
   `onSettingsChange { current -> current.copy(...) }`，由 `updateAppSettings` 在 DataStore
   的锁内读最新值再改。书库那边的 `toggleNoteLayout` 本来就是这么写的，所以只有笔记页出问题。
2. **相邻 32dp 圆钮至少留 16dp。** M3 的最小触控区是 48dp，32dp 的圆钮每边向外扩 8dp；
   间距只有 8dp 时两个触控区重叠 8dp，点在排版钮右缘会打到排序钮。
3. **当前主页要能跨 Activity 重建恢复。** `homeTarget`、`homeSection`、`notesTag` 用
   `rememberSaveable`，不要只用 `remember`。否则改系统字号、系统日夜（部分机型）、语言等
   触发重建时，页面会退回「启动页」设置（多半是书库）——用户在全部笔记按排版，改到的自然是
   书库。`drawerOpen` 这类瞬时状态不保存。

**验收**：在全部笔记连点切换 N 次，结果与 N 的奇偶一致；改系统字号触发重建后仍停在全部笔记；
点排版钮的最右缘不再弹出排序。

### 13.9 外部開啟（檔案關聯）

「用其他應用程式開啟」的清單**完全由 manifest 的 `<intent-filter>` 決定**：沒有宣告就永遠不會出現，
跟權限、套件可見性無關；`<queries>` 只影響我們去查別人的 App，不影響別人看到我們。

- **型別組 + 後綴組都要有**。型別組（`application/pdf`、DOCX 的 OOXML 長型別、`text/html`）是主路徑；
  聊天軟體常丟 `application/octet-stream` 或完全不帶型別，只能靠 `pathPattern` 比對後綴。
  `pathPattern` 區分大小寫，大小寫各寫一條。
- **後綴比對有先天限制**：它比的是 Uri 的 path。`content://…/document/1234` 這種不透明 ID
  根本看不到檔名，只能靠型別；所以型別組不能省。
- **不要宣告解析不了的格式**：`.doc`（舊二進位）、`.rtf`、`.odt` 目前沒有解析器，宣告了只會讓使用者
  選到一個必然失敗的入口。等 `DocumentImporter` 真的支援再加。
- **DOCX／XLSX 也是 zip**：`BookFormat.sniffHead` 不能看到 `PK` 就回 EPUB。標準 EPUB 的第一個
  條目是未壓縮的 `mimetype`，`application/epub+zip` 會落在前 80 bytes；沒有這個字串就交給匯入器。
  另外 `openDocument` 只要副檔名認得是匯入格式（docx/pdf/html/md/txt），就不再 sniff。
- **分流規則**：書（EPUB／MOBI／AZW3／FB2）進閱讀器；其餘支援的格式匯入成筆記。PDF 匯入時要
  `takePersistableUriPermission`，否則編輯頁那顆「打開原始 PDF」下次啟動必定失敗。

### 13.10 新功能自检清单（每次开工照着走）

1. 这个功能是使用者明确要的吗？没有要求就不要自造整套模块。
2. 状态从哪来？会被 Nav3 缓存吗？entry 内容里读的是 `State` 还是普通值？
   写设置传的是 transform 还是组合快照？当前页跨重建能恢复吗？
3. 窗口与 insets：状态栏、导航栏、短横屏、分屏、200% 字号都过了吗？
4. 列表/网格：列数按真实约束算了吗？有没有单测钉住？
5. 文案：四语齐了吗？`HelpContent` 同步了吗？
6. 交互：原生与自绘反馈有没有重复？禁用/选中的语义对吗？触控区域 ≥48dp 吗？
7. 数据层：有没有中文句子？错误码齐全吗？
8. 若删除或改名：13.6 的清单走完了吗？
9. 门禁：`assembleDebug` + `test` + `lintDebug`，lint error 必须为 0。
10. 真机：**经使用者同意**才安装；装完截图核对，改过的系统设置要还原。
11. **文档与代码冲突时以实测为准**：先改文档再改代码，不能两套说法并行。
    本次的教训就是上一版文档写着「180dp 保留手机双列体验」，实测只有一列；
    照文档写只会把同一个 bug 再抄一遍。
