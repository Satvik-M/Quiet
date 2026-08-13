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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.data.settings.AppFontFamily
import com.satvikm.quiet.data.settings.FontSize
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.data.settings.ThemeMode
import com.satvikm.quiet.domain.model.LaunchableApp

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
    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "Back",
            color = onBackground,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 24.dp),
        )

        Text(text = "Settings", color = onBackground, style = MaterialTheme.typography.headlineSmall)

        SettingRow(
            label = "Theme",
            options = listOf("System", "Light", "Dark"),
            selectedIndex = themeMode.ordinal,
            onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
        )
        SettingRow(
            label = "Font",
            options = listOf("Sans", "Monospace"),
            selectedIndex = fontFamily.ordinal,
            onSelect = { viewModel.setFontFamily(AppFontFamily.entries[it]) },
        )
        SettingRow(
            label = "Text size",
            options = listOf("Small", "Medium", "Large"),
            selectedIndex = fontSize.ordinal,
            onSelect = { viewModel.setFontSize(FontSize.entries[it]) },
        )
        SettingRow(
            label = "Alignment",
            options = listOf("Left", "Center"),
            selectedIndex = alignment.ordinal,
            onSelect = { viewModel.setAlignment(HomeAlignment.entries[it]) },
        )

        Text(
            text = "Gestures",
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        GestureRow(
            label = "Swipe left",
            app = swipeLeftApp,
            onChange = { onPickGestureApp(GestureSlot.SWIPE_LEFT) },
            onClear = { viewModel.clearGestureApp(GestureSlot.SWIPE_LEFT) },
        )
        GestureRow(
            label = "Swipe right",
            app = swipeRightApp,
            onChange = { onPickGestureApp(GestureSlot.SWIPE_RIGHT) },
            onClear = { viewModel.clearGestureApp(GestureSlot.SWIPE_RIGHT) },
        )

        Text(
            text = "Friction",
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        if (blockedApps.isEmpty()) {
            Text(
                text = "Long-press an app in the drawer to add friction",
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
            text = "Usage",
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        SettingRow(
            label = "Screen time on home",
            options = listOf("Off", "On"),
            selectedIndex = if (showScreenTime) 1 else 0,
            onSelect = { viewModel.setShowScreenTime(it == 1) },
        )
        Text(
            text = "View usage stats",
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
                text = "Remove",
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onRemove),
            )
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = "Delay: ${if (app.delaySeconds == 0) "off" else "${app.delaySeconds}s"}",
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable(onClick = onCycleDelay)
                    .padding(end = 20.dp),
            )
            Text(
                text = "Daily limit: ${app.dailyOpenLimit?.toString() ?: "none"}",
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onCycleDailyLimit),
            )
        }
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
                text = app?.displayLabel ?: "Not set",
                color = onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (app != null) {
            Text(
                text = "Clear",
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onClear),
            )
        }
    }
}
