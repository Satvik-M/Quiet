package com.satvikm.quiet.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.domain.model.LaunchableApp
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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

        FavoritesList(
            favorites = favorites,
            onLaunch = viewModel::launch,
            onReorder = viewModel::reorderFavorites,
        )

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

@Composable
private fun FavoritesList(
    favorites: List<LaunchableApp>,
    onLaunch: (LaunchableApp) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    var items by remember { mutableStateOf(favorites) }
    // Resync with the source of truth whenever it changes from outside a
    // drag (e.g. a favorite added/removed elsewhere).
    LaunchedEffect(favorites) { items = favorites }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(1f) }

    Column {
        items.forEachIndexed { index, app ->
            // Keying by app.id (not slot position) is essential: without
            // it, reordering mid-drag makes Compose think this slot's
            // *content* changed rather than the item moving, which cancels
            // and restarts the pointerInput coroutine — killing the drag
            // gesture before onDragEnd ever fires.
            key(app.id) {
                val offsetY = if (index == draggedIndex) dragOffsetY.roundToInt() else 0
                Text(
                    text = app.displayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { rowHeightPx = it.size.height.toFloat() }
                        .offset { IntOffset(0, offsetY) }
                        .pointerInput(app.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    // Look up the current index rather than
                                    // trusting the closure's `index`, which
                                    // can go stale: this pointerInput block
                                    // survives reorders (see key() above),
                                    // so it keeps running the instance from
                                    // whenever it was first composed.
                                    draggedIndex = items.indexOfFirst { it.id == app.id }
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val moveBy = (dragOffsetY / rowHeightPx).roundToInt()
                                    if (moveBy != 0) {
                                        val from = draggedIndex
                                        val to = (from + moveBy).coerceIn(0, items.lastIndex)
                                        if (to != from) {
                                            items = items.toMutableList().apply { add(to, removeAt(from)) }
                                            dragOffsetY -= moveBy * rowHeightPx
                                            draggedIndex = to
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                    onReorder(items.map { it.id })
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                },
                            )
                        }
                        .clickable { onLaunch(app) }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}
