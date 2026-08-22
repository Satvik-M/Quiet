package com.satvikm.quiet.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.R
import com.satvikm.quiet.data.usage.DailyUsage
import com.satvikm.quiet.ui.common.SettingRow
import com.satvikm.quiet.util.usageAccessSettingsIntent
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun UsageScreen(onBack: () -> Unit, viewModel: UsageViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val granted by viewModel.usageAccessGranted.collectAsStateWithLifecycle()
    val labels by viewModel.appLabels.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val topApps by viewModel.topApps.collectAsStateWithLifecycle()
    val avgMillisPerDay by viewModel.avgMillisPerDay.collectAsStateWithLifecycle()
    val avgUnlocksPerDay by viewModel.avgUnlocksPerDay.collectAsStateWithLifecycle()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val onBackground = MaterialTheme.colorScheme.onBackground

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.back),
            color = onBackground,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 24.dp),
        )

        Text(text = stringResource(R.string.today_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)

        if (!granted) {
            Text(
                text = stringResource(R.string.grant_usage_access),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { context.startActivity(usageAccessSettingsIntent()) }
                    .padding(top = 16.dp),
            )
            return@Column
        }

        Text(
            text = formatDuration(daily.totalMillis),
            color = onBackground,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.unlocks_count, daily.unlockCount, daily.unlockCount),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        daily.perApp.forEach { usage ->
            if (usage.foregroundMillis < 60_000) return@forEach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = labels[usage.packageName] ?: usage.packageName, color = onBackground)
                Text(text = formatDuration(usage.foregroundMillis), color = onBackground.copy(alpha = 0.7f))
            }
        }

        SettingRow(
            label = stringResource(R.string.period_label),
            options = listOf(stringResource(R.string.period_week), stringResource(R.string.period_month)),
            selectedIndex = period.ordinal,
            onSelect = { viewModel.setPeriod(UsagePeriod.entries[it]) },
        )

        Text(
            text = stringResource(if (period == UsagePeriod.WEEK) R.string.week_title else R.string.month_title),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        val maxRangeMillis = range.maxOfOrNull { it.second.totalMillis }?.coerceAtLeast(1L) ?: 1L
        range.forEach { (date, usage) ->
            TrendDayRow(date = date, usage = usage, maxMillis = maxRangeMillis, showDayOfMonth = period == UsagePeriod.MONTH)
        }
        Text(
            text = stringResource(R.string.avg_per_day_label, formatDuration(avgMillisPerDay)),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.avg_unlocks_label, avgUnlocksPerDay),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
        )

        if (topApps.isNotEmpty()) {
            Text(
                text = stringResource(R.string.most_used_title),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            topApps.forEachIndexed { index, usage ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${index + 1}. ${labels[usage.packageName] ?: usage.packageName}",
                        color = onBackground,
                    )
                    Text(text = formatDuration(usage.foregroundMillis), color = onBackground.copy(alpha = 0.7f))
                }
            }
        }

        val goalOptions = listOf(null, 60, 120, 180, 240)
        SettingRow(
            label = stringResource(R.string.daily_goal_label),
            options = listOf(
                stringResource(R.string.not_set),
                stringResource(R.string.duration_hours, 1),
                stringResource(R.string.duration_hours, 2),
                stringResource(R.string.duration_hours, 3),
                stringResource(R.string.duration_hours, 4),
            ),
            selectedIndex = goalOptions.indexOf(dailyGoalMinutes).coerceAtLeast(0),
            onSelect = { viewModel.setDailyGoalMinutes(goalOptions[it]) },
        )
        if (dailyGoalMinutes != null && streak > 0) {
            Text(
                text = stringResource(R.string.streak_label, streak),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TrendDayRow(date: LocalDate, usage: DailyUsage, maxMillis: Long, showDayOfMonth: Boolean) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val label = if (showDayOfMonth) date.dayOfMonth.toString() else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val fraction = (usage.totalMillis.toFloat() / maxMillis.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(32.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .padding(horizontal = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(onBackground.copy(alpha = 0.25f)),
            )
        }
        Text(
            text = formatDuration(usage.totalMillis),
            color = onBackground.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}
