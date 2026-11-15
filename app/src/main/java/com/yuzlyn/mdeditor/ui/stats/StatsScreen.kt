package com.yuzlyn.mdeditor.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.stats.StatsDates
import com.yuzlyn.mdeditor.data.stats.StatsItem
import com.yuzlyn.mdeditor.data.stats.StatsSnapshot
import com.yuzlyn.mdeditor.data.stats.StatKind
import com.yuzlyn.mdeditor.data.profile.ProfileData
import com.yuzlyn.mdeditor.ui.component.Appear
import com.yuzlyn.mdeditor.ui.component.RailButton
import com.yuzlyn.mdeditor.ui.screen.Avatar
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space

/** 統計頁的時間窗。用「最近 N 天」而不是日曆週／月：跨月的那一週不會忽然少一半。 */
private enum class StatsRange(val days: Long) {
    WEEK(7),
    MONTH(30),
    YEAR(365)
}

/**
 * 統計頁：一頁看完「讀了多久、讀了什麼、寫了多少」。
 *
 * 三個區塊都受設定控制（[AppSettings.statsShowHeatmap] / [AppSettings.statsShowRanking]），
 * 總開關關掉時整頁換成一個說明畫面——不是留一個空頁面讓人以為壞了。
 *
 * 版面在大螢幕上收斂到 840dp：熱力圖與排行榜都是橫向資訊，鋪滿整片平板反而更難掃視。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    snapshot: StatsSnapshot,
    settings: AppSettings,
    /** 個人資料：統計頁頂部要用暱稱與頭像打招呼。 */
    profile: ProfileData = ProfileData(),
    onOpenDrawer: () -> Unit,
    onOpenStatsSettings: () -> Unit,
    onEnableStats: () -> Unit,
    onOpenBook: (String, String) -> Unit = { _, _ -> },
    onOpenNote: (String) -> Unit = {},
    /** 點頭像可以回到名片頁補資料；預設不做任何事，讓呼叫端自己決定。 */
    onOpenProfile: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var range by remember { mutableStateOf(StatsRange.WEEK) }

    val today = StatsDates.today()
    val from = StatsDates.daysAgo(range.days - 1)
    val totals = remember(snapshot, from, today) { snapshot.totals(from, today) }
    val series = remember(snapshot, from, today, range) {
        distribution(snapshot, range, from, today)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StatsHeader(
            subtitle = strings.statsRangeLabel(range.days),
            onOpenDrawer = onOpenDrawer,
            onOpenStatsSettings = onOpenStatsSettings
        )

        if (!settings.statsEnabled) {
            StatsDisabled(strings.statsDisabled, strings.statsDisabledHint, strings.statsEnable, onEnableStats)
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.s200,
                end = Space.s200,
                top = Space.s100,
                bottom = Space.s400
            ),
            verticalArrangement = Arrangement.spacedBy(Space.s200),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                NarrowColumn {
                    GreetingCard(
                        nickname = profile.nickname.trim(),
                        avatarPath = profile.avatarPath,
                        onOpenProfile = onOpenProfile
                    )
                }
            }

            item {
                NarrowColumn {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        StatsRange.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = range == option,
                                onClick = { range = option },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = StatsRange.entries.size
                                ),
                                label = { Text(strings.statsRangeLabel(option.days)) }
                            )
                        }
                    }
                }
            }

            item {
                NarrowColumn {
                    HeroCard(
                        seconds = totals.readSeconds,
                        summary = buildString {
                            append(strings.statsBooksCount(totals.booksRead))
                            append(" · ")
                            append(strings.statsDocsCount(totals.docsEdited))
                        }
                    )
                }
            }

            item {
                NarrowColumn {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s150)) {
                        StatTile(
                            label = strings.statsBooks,
                            value = totals.booksRead.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = strings.statsDocs,
                            value = totals.docsOpened.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = strings.statsEdits,
                            value = totals.editCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                NarrowColumn {
                    SectionCard(
                        title = if (range == StatsRange.YEAR) strings.statsMonthly else strings.statsDistribution
                    ) {
                        DistributionStrip(
                            values = series.values,
                            startLabel = series.startLabel,
                            endLabel = series.endLabel
                        )
                    }
                }
            }

            if (settings.statsShowHeatmap) {
                item {
                    NarrowColumn {
                        HeatmapCard(
                            hint = strings.statsActivityHint,
                            less = strings.statsLess,
                            more = strings.statsMore,
                            activity = remember(snapshot, today) {
                                snapshot.activity(StatsDates.daysAgo(364), today)
                            }
                        )
                    }
                }
            }

            if (settings.statsShowRanking) {
                item {
                    NarrowColumn {
                        RankingSection(
                            snapshot = snapshot,
                            from = from,
                            to = today,
                            onOpenBook = onOpenBook,
                            onOpenNote = onOpenNote
                        )
                    }
                }
            }

            if (totals.isEmpty && snapshot.isEmpty) {
                item {
                    NarrowColumn {
                        EmptyCard(strings.statsEmpty, strings.statsEmptyHint)
                    }
                }
            }
        }
    }
}

/** 大螢幕收斂寬度；手機上等於滿版。 */
@Composable
private fun NarrowColumn(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun StatsHeader(
    subtitle: String,
    onOpenDrawer: () -> Unit,
    onOpenStatsSettings: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Space.s150, vertical = Space.s100),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s150)
    ) {
        RailButton(icon = Icons.Default.Menu, description = strings.menu, onClick = onOpenDrawer)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.statsTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RailButton(
            icon = Icons.Default.Tune,
            description = strings.statsSettingsTitle,
            onClick = onOpenStatsSettings
        )
    }
}

/**
 * 頂部問候：「嗨，yuzlyn」＋頭像。
 *
 * 統計頁原本一開頭就是數字，冷冰冰的；先叫出使用者的名字，
 * 這頁才像「你的」統計，而不是一份報表。沒有設暱稱時退回一句通用的招呼，
 * 不會露出一段空白或半個 `嗨，`。
 *
 * 頭像可以點：直接跳到名片頁補資料，不用先記住入口在哪個抽屜。
 */
@Composable
private fun GreetingCard(nickname: String, avatarPath: String, onOpenProfile: () -> Unit) {
    val strings = LocalAppStrings.current
    Appear(index = 0) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(Space.s250),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s200)
            ) {
                Avatar(
                    avatarPath = avatarPath,
                    size = Space.s700,
                    contentDescription = strings.profileTitle,
                    onClick = onOpenProfile
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nickname.isEmpty()) {
                            strings.statsGreetingGuest
                        } else {
                            strings.statsGreeting(nickname)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.statsGreetingHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/** 「閱讀時長」那一塊：整個頁面唯一用主色填滿的地方，一眼就知道重點在哪。 */
@Composable
private fun HeroCard(seconds: Long, summary: String) {
    val strings = LocalAppStrings.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Space.s300)) {
            Text(
                text = strings.statsReadingTime,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
            Text(
                text = strings.statsDuration(seconds),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Space.s100))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f)
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(Space.s200)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Space.s200),
            verticalArrangement = Arrangement.spacedBy(Space.s150)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Space.s300)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(Space.s50))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsDisabled(title: String, body: String, action: String, onEnable: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .padding(Space.s300),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s150)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onEnable) { Text(action) }
        }
    }
}

// ── 分佈長條 ─────────────────────────────────────────────────────────────

private class Series(
    val values: List<Long>,
    val startLabel: String,
    val endLabel: String
)

/**
 * 期間內的分佈：7 天／30 天是逐日，一年是逐月。
 *
 * 這裡刻意不畫座標軸、不做縮放——它要回答的是「哪天我有在動」，
 * 不是「精確讀了幾秒」。要看數字看上面的總量就好。
 */
private fun distribution(
    snapshot: StatsSnapshot,
    range: StatsRange,
    from: String,
    to: String
): Series {
    val activity = snapshot.activity(from, to)
    return when (range) {
        StatsRange.WEEK, StatsRange.MONTH -> {
            val days = (0 until range.days).map { StatsDates.plusDays(from, it) }
            Series(
                values = days.map { activity[it]?.score ?: 0L },
                startLabel = StatsDates.shortLabel(from),
                endLabel = StatsDates.shortLabel(to)
            )
        }

        StatsRange.YEAR -> {
            // 逐月加總：一天一根在一年的尺度上會細到看不見。
            val months = LinkedHashMap<String, Long>()
            (0 until StatsDates.daysBetween(from, to) + 1).forEach { offset ->
                val day = StatsDates.plusDays(from, offset.toLong())
                val month = day.substring(0, 7)
                months[month] = (months[month] ?: 0L) + (activity[day]?.score ?: 0L)
            }
            Series(
                values = months.values.toList(),
                startLabel = StatsDates.monthLabel(from),
                endLabel = StatsDates.monthLabel(to)
            )
        }
    }
}

@Composable
private fun DistributionStrip(values: List<Long>, startLabel: String, endLabel: String) {
    val colors = MaterialTheme.colorScheme
    val max = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(Space.s100)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(Space.s700)
        ) {
            if (values.isEmpty()) return@Canvas
            val gap = 3.dp.toPx()
            val width = (size.width - gap * (values.size - 1)) / values.size
            val radius = CornerRadius(width.coerceAtMost(6.dp.toPx()) / 2f)
            values.forEachIndexed { index, value ->
                // 有紀錄的那一天至少留 3dp：不然 1 分鐘和 0 分鐘看起來一樣。
                val height = if (value <= 0) {
                    2.dp.toPx()
                } else {
                    (size.height * (value.toFloat() / max)).coerceAtLeast(3.dp.toPx())
                }
                drawRoundRect(
                    color = if (value <= 0) {
                        colors.onSurface.copy(alpha = 0.10f)
                    } else {
                        colors.primary.copy(alpha = 0.35f + 0.65f * (value.toFloat() / max))
                    },
                    topLeft = Offset(index * (width + gap), size.height - height),
                    size = Size(width, height),
                    cornerRadius = radius
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── 熱力圖 ───────────────────────────────────────────────────────────────

private const val HEATMAP_WEEKS = 53
private const val HEATMAP_DAYS = HEATMAP_WEEKS * 7

/**
 * GitHub 那種格子圖。
 *
 * 53 欄 × 7 列排下來大約 740dp，手機放不下——所以橫向捲動，並且一進來就停在
 * 最右邊（最近的那一週）。捲到最左邊是一年前，那個方向是「回顧」，不該是預設視野。
 */
@Composable
private fun HeatmapCard(
    hint: String,
    less: String,
    more: String,
    activity: Map<String, com.yuzlyn.mdeditor.data.stats.StatsDay>
) {
    val colors = MaterialTheme.colorScheme
    val today = StatsDates.today()
    val start = StatsDates.startOfWeek(StatsDates.plusDays(today, -(HEATMAP_DAYS.toLong() - 1)))
    val weeks = remember(start, today) {
        (0 until HEATMAP_WEEKS).map { week ->
            (0 until 7).map { day ->
                StatsDates.plusDays(start, (week * 7 + day).toLong())
            }
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Space.s200),
            verticalArrangement = Arrangement.spacedBy(Space.s150)
        ) {
            Text(
                text = LocalAppStrings.current.statsActivity,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )

            val scroll = rememberScrollState()
            // 以 maxValue 為 key：第一次組合時它還是 0（版面還沒量），
            // 量完才會變大，那時再捲一次才真的捲得動。
            LaunchedEffect(scroll.maxValue) {
                if (scroll.maxValue > 0) scroll.scrollTo(scroll.maxValue)
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // 星期標籤只留一、三、五：七列全標在手機上會擠成一團。
                Column(modifier = Modifier.width(18.dp)) {
                    Spacer(modifier = Modifier.height(14.dp))
                    listOf("一", "", "三", "", "五", "", "").forEach { label ->
                        Box(modifier = Modifier.height(14.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f
                            )
                        }
                    }
                }
                Column(modifier = Modifier.horizontalScroll(scroll)) {
                    MonthLabels(weeks)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        weeks.forEach { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                week.forEach { day ->
                                    HeatCell(day, today) { activity[day]?.score ?: 0L }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(less, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Spacer(modifier = Modifier.width(Space.s100))
                (0..4).forEach { level ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(10.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(levelColor(level))
                    )
                }
                Spacer(modifier = Modifier.width(Space.s100))
                Text(more, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }

            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthLabels(weeks: List<List<String>>) {
    val colors = MaterialTheme.colorScheme
    // 一格只有 11dp，直接塞進格子裡會被裁成「11」。改成在整條上**絕對定位**：
    // 月份標籤本來就只出現在月界，一年十二個，彼此至少隔四週，不會撞在一起。
    Box(
        modifier = Modifier
            .width(14.dp * weeks.size.toFloat())
            .height(14.dp)
    ) {
        var previous = ""
        weeks.forEachIndexed { index, week ->
            val month = week.first().substring(0, 7)
            if (month != previous) {
                Text(
                    text = StatsDates.monthLabel(week.first()),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = 14.dp * index.toFloat())
                )
            }
            previous = month
        }
    }
}

@Composable
private fun levelColor(level: Int): Color {
    val colors = MaterialTheme.colorScheme
    return when (level) {
        // 空格不用主色淡彩，改用中性底：整片格子才不會被「讀過一點」和「完全沒讀」糊在一起。
        0 -> colors.onSurface.copy(alpha = 0.08f)
        1 -> colors.primary.copy(alpha = 0.28f)
        2 -> colors.primary.copy(alpha = 0.48f)
        3 -> colors.primary.copy(alpha = 0.72f)
        else -> colors.primary
    }
}

@Composable
private fun HeatCell(day: String, today: String, score: () -> Long) {
    // 未來的日子不畫格子：一年 53 週一定有一天會超過今天。
    val future = day > today
    Box(
        modifier = Modifier
            .size(11.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(if (future) Color.Transparent else levelColor(levelOf(score())))
    )
}

/** 活躍度 → 五級。門檻用「分鐘＋次數」的絕對值，跨天之間才可比。 */
private fun levelOf(score: Long): Int = when {
    score <= 0L -> 0
    score < 15 -> 1
    score < 40 -> 2
    score < 90 -> 3
    else -> 4
}

// ── 排行榜 ───────────────────────────────────────────────────────────────

@Composable
private fun RankingSection(
    snapshot: StatsSnapshot,
    from: String,
    to: String,
    onOpenBook: (String, String) -> Unit,
    onOpenNote: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var showBooks by remember { mutableStateOf(true) }

    val items = remember(snapshot, from, to, showBooks) {
        if (showBooks) {
            snapshot.ranking(from, to, StatKind.READ)
        } else {
            snapshot.ranking(from, to, StatKind.EDIT)
        }
    }
    val max = items.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Space.s200),
            verticalArrangement = Arrangement.spacedBy(Space.s150)
        ) {
            Text(
                text = strings.statsRanking,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s100)) {
                FilterChip(
                    selected = showBooks,
                    onClick = { showBooks = true },
                    label = { Text(strings.statsRankingBooks) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                FilterChip(
                    selected = !showBooks,
                    onClick = { showBooks = false },
                    label = { Text(strings.statsRankingDocs) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            if (items.isEmpty()) {
                Text(
                    text = strings.statsEmptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEachIndexed { index, item ->
                    RankingRow(
                        rank = index + 1,
                        item = item,
                        fraction = item.value.toFloat() / max,
                        valueText = if (showBooks) {
                            strings.statsDuration(item.value)
                        } else {
                            strings.statsTimesCount(item.value.toInt())
                        },
                        meta = if (showBooks) {
                            strings.statsStartFrom(StatsDates.shortLabel(item.firstDay))
                        } else {
                            strings.statsStartFrom(StatsDates.shortLabel(item.lastDay))
                        },
                        onClick = if (showBooks) {
                            { onOpenBook(item.key, item.name) }
                        } else {
                            { onOpenNote(item.key) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingRow(
    rank: Int,
    item: StatsItem,
    fraction: Float,
    valueText: String,
    meta: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(Space.s150),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s150)
        ) {
            Box(
                modifier = Modifier
                    .size(Space.s400)
                    .clip(CircleShape)
                    .background(
                        if (rank <= 3) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (rank <= 3) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(Space.s75))
                // 自己畫而不是用 LinearProgressIndicator：只有 4dp 高的細條，
                // 官方元件的內建留白會讓它在列裡顯得歪一邊。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** 頁首副標：目前看的是哪一段。 */
private fun com.yuzlyn.mdeditor.ui.i18n.AppStrings.statsRangeLabel(days: Long): String = when (days) {
    7L -> statsRange7
    30L -> statsRange30
    else -> statsRange365
}
