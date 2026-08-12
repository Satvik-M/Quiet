package com.satvikm.quiet.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter

/** A swipe up this far (px) opens the drawer. */
private const val OPEN_DRAWER_DRAG_THRESHOLD = -120f

@Composable
fun HomeScreen(onOpenDrawer: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val now by viewModel.currentTime.collectAsStateWithLifecycle()
    val batteryPercent by viewModel.batteryPercent.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    val timeFormatter = remember(context) {
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm")
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        if (totalDrag < OPEN_DRAWER_DRAG_THRESHOLD) onOpenDrawer()
                    },
                )
            }
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Text(text = now.format(timeFormatter), style = MaterialTheme.typography.displayMedium, color = onBackground)
        Text(text = now.format(dateFormatter), style = MaterialTheme.typography.bodyLarge, color = onBackground)
        Text(text = "$batteryPercent%", style = MaterialTheme.typography.bodyMedium, color = onBackground)

        Spacer(modifier = Modifier.padding(top = 32.dp))

        Column {
            favorites.forEach { app ->
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.launch(app) }
                        .padding(vertical = 10.dp),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "All apps",
            color = onBackground,
            modifier = Modifier
                .clickable(onClick = onOpenDrawer)
                .padding(vertical = 8.dp),
        )
    }
}
