# ROADMAP

> 按 Google 的 OKR 写法：一个季度一个 Objective，配三个可量化的 Key Result。
> **0.7 就算成功**——如果每个 KR 都拿满分，说明目标定低了。
> 季度末在这里打一次分，然后新建一份，不要在这份上改。

## 当前周期：2026 Q4（11/28 – 12/31，五周）

### O：让 2.2 成为一个「别人能接手」的版本

功能面已经收敛，这一季不加新东西。目标是把维护成本降下来：
出问题时能快速定位，换台机器能跑通全部验证，代码不再是一份只有自己能读的文本。

| # | Key Result | 起点 | 目标 | 怎么算完成 |
|---|---|---|---|---|
| KR1 | lint 告警收敛 | 62 warning | ≤ 30 warning，error 恒为 0 | `./gradlew lintDebug` 报告 |
| KR2 | 测试样本单一来源 | 两份手工副本 | 只留一份源，零手工同步 | 连跑两次生成脚本，`git diff` 为空 |
| KR3 | Release 可混淆 | `isMinifyEnabled = false` | R8 开启，216 用例全绿 + 真机冒烟 | `assembleRelease` 产物能装能开 |

### KR1：数量是结果，不是目的

62 条 warning 里大部分无害（未使用资源、可空性标注）。**不要逐条清**——
按 issue id 分组，只处理「可能掩盖真 bug」的那几类：`NewApi`、
被反射引用的 `UnusedResources`、`MissingTranslation`。
其余写进 `lint.xml` 显式 ignore，并在注释里写明为什么不修。
真正的目的是：让**下次新增**的 warning 能被一眼看见。

### KR2：两份样本的 md5 完全一致

2026-11-28 实测：`app/src/test/resources/books/` 与
`app/src/androidTest/assets/books/` 下三个文件的校验和逐一相同。
所以不需要「同步」，只需要**合并**：

```kotlin
android {
    sourceSets.getByName("androidTest") {
        assets.srcDir("src/test/resources")
    }
}
```

然后删掉 `app/src/androidTest/assets/books/`。
顺带把 `tools/make_test_books.py` 的 zip 时间戳固定成常量
（第 111–118 行的 `writestr` 走字符串路径，会用 `time.localtime()`；
改用显式 `ZipInfo` 并设 `date_time=(1980, 1, 1, 0, 0, 0)`），
否则每跑一次都产生二进制 diff。

### KR3：混淆会咬掉三样东西

R8 会重命名 Room 实体、Kotlin 反射用到的类名、以及 WebView 桥接的方法名。
**`@JavascriptInterface` 尤其危险**：混淆后 JS 侧调不到，功能静默失效，
不报错、不崩溃，只是按钮点了没反应。

做法：开分支 → `isMinifyEnabled = true` + `shrinkResources = true`
→ 跑完 216 个 JVM 用例 → 真机把「预览、剪贴板、导入、导出、阅读器」
五条链路各走一遍。任何一条不通就补 keep 规则，**不要关混淆**。

## 怎么用这份文件

- 每周五分钟：这周做的事，和三个 KR 有关系吗？没有就是在还债或跑偏。
- 「不做的事」也要写下来，否则过两个月又会重新起意。
- 季度末打分（0 / 0.3 / 0.7 / 1.0），写进下面，然后开新的一份。

## 本季度明确不做

- **不加新功能。** 写作、导入、阅读、导出、统计五块已经覆盖完毕。
  （2026-11-28 例外：作者要求新增「检查更新」。这是一次有意识的产品决策，不是开口子；
  再加任何功能仍须先改本节。）
- **不做云同步、不做账号。** 「数据不出设备」仍是产品承诺的主体：笔记、书库、统计、
  阅读记录都不上传。唯一例外是手动「检查更新」——按下按钮时向 GitHub 询问一次
  正式版版本号，请求不携带任何本机数据，也不在启动或后台发起。`AndroidManifest.xml`
  因此有 `INTERNET`；除此之外不得再发任何请求，新增第二条联网行为就等于改产品定义。
- **不上架 Google Play。** 签名沿用 debug 证书（v1.0.0 起同一张），这是既定策略。

## 计分（季度末填）

| KR | 得分 | 说明 |
|---|---|---|
| KR1 | 1.0 | 62 warning + 12 hint → 28 warning，error 保持 0 |
| KR2 | 1.0 | 两份副本合并为一份，脚本幂等，连跑两次校验和一致 |
| KR3 | 0.7 | R8 已开、体积 35.9 → 21.3 MB、映射已验证；真机五条链路未跑 |

## 历史

- **2026 Q4**：首份 ROADMAP，建于 2026-11-28。此前 17 个提交、5 个月开发，
  功能从零做到 2.1.2，没有书面目标，靠记忆驱动。
