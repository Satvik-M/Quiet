package com.satvikm.quiet.ui.digest

import android.text.format.DateFormat
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.R
import com.satvikm.quiet.data.notifications.MutedNotificationEntity
import java.util.Date

@Composable
fun DigestScreen(onBack: () -> Unit, viewModel: DigestViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val labels by viewModel.appLabels.collectAsStateWithLifecycle()
    val onBackground = MaterialTheme.colorScheme.onBackground
    val timeFormat = DateFormat.getTimeFormat(context)

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

        Text(text = stringResource(R.string.digest_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)

        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.digest_empty_hint),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            entries.forEach { entry ->
                DigestRow(
                    entry = entry,
                    appLabel = labels[entry.packageName] ?: entry.packageName,
                    timeLabel = timeFormat.format(Date(entry.timestampMillis)),
                )
            }
        }
    }
}

@Composable
private fun DigestRow(entry: MutedNotificationEntity, appLabel: String, timeLabel: String) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = appLabel,
                color = onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = timeLabel,
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val title = entry.title
        val text = entry.text
        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.digest_no_content),
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        } else {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!text.isNullOrBlank()) {
                Text(
                    text = text,
                    color = onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
