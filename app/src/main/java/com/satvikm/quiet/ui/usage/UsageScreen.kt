package com.satvikm.quiet.ui.usage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.satvikm.quiet.util.usageAccessSettingsIntent

@Composable
fun UsageScreen(onBack: () -> Unit, viewModel: UsageViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val daily by viewModel.daily.collectAsStateWithLifecycle()
    val granted by viewModel.usageAccessGranted.collectAsStateWithLifecycle()
    val labels by viewModel.appLabels.collectAsStateWithLifecycle()
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
