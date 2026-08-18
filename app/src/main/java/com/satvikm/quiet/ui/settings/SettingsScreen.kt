package com.satvikm.quiet.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.R
import com.satvikm.quiet.data.focus.FocusScheduleEntity
import com.satvikm.quiet.data.settings.AppFontFamily
import com.satvikm.quiet.data.settings.FontSize
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.data.settings.ThemeMode
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.util.grayscaleGrantCommand
import com.satvikm.quiet.util.isNotificationAccessGranted
import com.satvikm.quiet.util.isWriteSecureSettingsGranted
import com.satvikm.quiet.util.notificationListenerSettingsIntent

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPickGestureApp: (GestureSlot) -> Unit,
    onOpenUsage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val alignment by viewModel.alignment.collectAsStateWithLifecycle()
    val showScreenTime by viewModel.showScreenTime.collectAsStateWithLifecycle()
    val swipeLeftApp by viewModel.swipeLeftApp.collectAsStateWithLifecycle()
    val swipeRightApp by viewModel.swipeRightApp.collectAsStateWithLifecycle()
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val mutedApps by viewModel.mutedApps.collectAsStateWithLifecycle()
    val showMutedCount by viewModel.showMutedCount.collectAsStateWithLifecycle()
    val grayscaleEnabled by viewModel.grayscaleEnabled.collectAsStateWithLifecycle()
    val focusSchedules by viewModel.focusSchedules.collectAsStateWithLifecycle()
    val onBackground = MaterialTheme.colorScheme.onBackground

    val context = LocalContext.current
    var notificationAccessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var writeSecureSettingsGranted by remember { mutableStateOf(isWriteSecureSettingsGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = isNotificationAccessGranted(context)
                writeSecureSettingsGranted = isWriteSecureSettingsGranted(context)
            }
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

        Text(text = stringResource(R.string.settings_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)

        SettingRow(
            label = stringResource(R.string.theme_label),
            options = listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
            selectedIndex = themeMode.ordinal,
            onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
        )
        SettingRow(
            label = stringResource(R.string.font_label),
            options = listOf(stringResource(R.string.font_sans), stringResource(R.string.font_monospace)),
            selectedIndex = fontFamily.ordinal,
            onSelect = { viewModel.setFontFamily(AppFontFamily.entries[it]) },
        )
        SettingRow(
            label = stringResource(R.string.text_size_label),
            options = listOf(stringResource(R.string.text_size_small), stringResource(R.string.text_size_medium), stringResource(R.string.text_size_large)),
            selectedIndex = fontSize.ordinal,
            onSelect = { viewModel.setFontSize(FontSize.entries[it]) },
        )
        SettingRow(
            label = stringResource(R.string.alignment_label),
            options = listOf(stringResource(R.string.alignment_left), stringResource(R.string.alignment_center)),
            selectedIndex = alignment.ordinal,
            onSelect = { viewModel.setAlignment(HomeAlignment.entries[it]) },
        )

        Text(
            text = stringResource(R.string.gestures_section),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        GestureRow(
            label = stringResource(R.string.swipe_left_label),
            app = swipeLeftApp,
            onChange = { onPickGestureApp(GestureSlot.SWIPE_LEFT) },
            onClear = { viewModel.clearGestureApp(GestureSlot.SWIPE_LEFT) },
        )
        GestureRow(
            label = stringResource(R.string.swipe_right_label),
            app = swipeRightApp,
            onChange = { onPickGestureApp(GestureSlot.SWIPE_RIGHT) },
            onClear = { viewModel.clearGestureApp(GestureSlot.SWIPE_RIGHT) },
        )

        Text(
            text = stringResource(R.string.friction_section),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        if (blockedApps.isEmpty()) {
            Text(
                text = stringResource(R.string.friction_empty_hint),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            blockedApps.forEach { app ->
                BlockedAppRow(
                    app = app,
                    onCycleDelay = { viewModel.cycleDelay(app) },
                    onCycleDailyLimit = { viewModel.cycleDailyLimit(app) },
                    onRemove = { viewModel.removeBlocked(app) },
                )
            }
        }

        Text(
            text = stringResource(R.string.focus_schedules_section),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        Text(
            text = stringResource(R.string.focus_schedules_hint),
            color = onBackground.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        focusSchedules.forEach { schedule ->
            FocusScheduleRow(
                schedule = schedule,
                onCycleStart = { viewModel.cycleStartHour(schedule) },
                onCycleEnd = { viewModel.cycleEndHour(schedule) },
                onToggleDay = { day -> viewModel.toggleDay(schedule, day) },
                onToggleEnabled = { viewModel.toggleScheduleEnabled(schedule) },
                onRemove = { viewModel.removeSchedule(schedule) },
            )
        }
        Text(
            text = stringResource(R.string.add_schedule),
            color = onBackground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable(onClick = viewModel::addFocusSchedule)
                .padding(top = 4.dp, bottom = 8.dp),
        )

        Text(
            text = stringResource(R.string.notifications_section),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        if (!notificationAccessGranted) {
            Text(
                text = stringResource(R.string.grant_notification_access),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { context.startActivity(notificationListenerSettingsIntent()) }
                    .padding(bottom = 8.dp),
            )
        } else if (mutedApps.isEmpty()) {
            Text(
                text = stringResource(R.string.muted_empty_hint),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            mutedApps.forEach { app ->
                MutedAppRow(app = app, onRemove = { viewModel.removeMuted(app) })
            }
        }
        SettingRow(
            label = stringResource(R.string.muted_count_on_home_label),
            options = listOf(stringResource(R.string.off), stringResource(R.string.on)),
            selectedIndex = if (showMutedCount) 1 else 0,
            onSelect = { viewModel.setShowMutedCount(it == 1) },
        )

        Text(
            text = stringResource(R.string.grayscale_section),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        if (!writeSecureSettingsGranted) {
            Text(
                text = stringResource(R.string.grayscale_permission_hint),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = grayscaleGrantCommand(context),
                color = onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            SettingRow(
                label = stringResource(R.string.system_wide_grayscale_label),
                options = listOf(stringResource(R.string.off), stringResource(R.string.on)),
                selectedIndex = if (grayscaleEnabled) 1 else 0,
                onSelect = { viewModel.setGrayscale(it == 1) },
            )
        }

        Text(
            text = stringResource(R.string.usage_section),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        SettingRow(
            label = stringResource(R.string.screen_time_on_home_label),
            options = listOf(stringResource(R.string.off), stringResource(R.string.on)),
            selectedIndex = if (showScreenTime) 1 else 0,
            onSelect = { viewModel.setShowScreenTime(it == 1) },
        )
        Text(
            text = stringResource(R.string.view_usage_stats),
            color = onBackground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable(onClick = onOpenUsage)
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = label, color = onBackground.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.padding(top = 4.dp)) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Text(
                    text = option,
                    color = if (selected) onBackground else onBackground.copy(alpha = 0.35f),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { onSelect(index) }
                        .padding(end = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun BlockedAppRow(
    app: BlockedAppUi,
    onCycleDelay: () -> Unit,
    onCycleDailyLimit: () -> Unit,
    onRemove: () -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = app.label,
                color = onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.remove),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onRemove),
            )
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = if (app.delaySeconds == 0) stringResource(R.string.delay_off) else stringResource(R.string.delay_seconds, app.delaySeconds),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable(onClick = onCycleDelay)
                    .padding(end = 20.dp),
            )
            Text(
                text = app.dailyOpenLimit?.let { stringResource(R.string.daily_limit_value, it) } ?: stringResource(R.string.daily_limit_none),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onCycleDailyLimit),
            )
        }
    }
}

@Composable
private fun FocusScheduleRow(
    schedule: FocusScheduleEntity,
    onCycleStart: () -> Unit,
    onCycleEnd: () -> Unit,
    onToggleDay: (Int) -> Unit,
    onToggleEnabled: () -> Unit,
    onRemove: () -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val dayLabels = stringArrayResource(R.array.day_abbreviations)
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.schedule_time_range, schedule.startHour, schedule.endHour),
                color = if (schedule.enabled) onBackground else onBackground.copy(alpha = 0.4f),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleEnabled),
            )
            Text(
                text = stringResource(R.string.remove),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onRemove),
            )
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = stringResource(R.string.schedule_start),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clickable(onClick = onCycleStart)
                    .padding(end = 16.dp),
            )
            Text(
                text = stringResource(R.string.schedule_end),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable(onClick = onCycleEnd),
            )
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            dayLabels.forEachIndexed { index, dayLabel ->
                val included = (schedule.daysMask and (1 shl index)) != 0
                Text(
                    text = dayLabel,
                    color = if (included) onBackground else onBackground.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onToggleDay(index) }
                        .padding(end = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun MutedAppRow(app: MutedAppUi, onRemove: () -> Unit) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = app.label,
            color = onBackground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.remove),
            color = onBackground.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onRemove),
        )
    }
}

@Composable
private fun GestureRow(
    label: String,
    app: LaunchableApp?,
    onChange: () -> Unit,
    onClear: () -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onChange),
        ) {
            Text(text = label, color = onBackground.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = app?.displayLabel ?: stringResource(R.string.not_set),
                color = onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (app != null) {
            Text(
                text = stringResource(R.string.clear),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onClear),
            )
        }
    }
}
