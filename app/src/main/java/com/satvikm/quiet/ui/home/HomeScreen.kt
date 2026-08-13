package com.satvikm.quiet.ui.home

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.data.settings.HomeAlignment
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.util.accessibilitySettingsIntent
import com.satvikm.quiet.util.expandNotifications
import com.satvikm.quiet.util.isGestureAccessibilityServiceEnabled
import com.satvikm.quiet.util.lockScreen
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** A drag past this far (px) on its dominant axis counts as a swipe. */
private const val SWIPE_THRESHOLD = 120f

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUsage: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val now by viewModel.currentTime.collectAsStateWithLifecycle()
    val batteryPercent by viewModel.batteryPercent.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val swipeLeftApp by viewModel.swipeLeftApp.collectAsStateWithLifecycle()
    val swipeRightApp by viewModel.swipeRightApp.collectAsStateWithLifecycle()
    val alignment by viewModel.alignment.collectAsStateWithLifecycle()
    val showScreenTime by viewModel.showScreenTime.collectAsStateWithLifecycle()
    val screenTimeMillis by viewModel.screenTimeMillis.collectAsStateWithLifecycle()

    val timeFormatter = remember(context) {
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm")
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    val onBackground = MaterialTheme.colorScheme.onBackground

    var accessibilityEnabled by remember { mutableStateOf(isGestureAccessibilityServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = isGestureAccessibilityServiceEnabled(context)
                viewModel.refreshScreenTime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!lockScreen()) toastAccessibilityNeeded(context, "lock the screen")
                    },
                )
            }
            .pointerInput(swipeLeftApp, swipeRightApp) {
                var totalDrag = Offset.Zero
                detectDragGestures(
                    onDragStart = { totalDrag = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        val absX = abs(totalDrag.x)
                        val absY = abs(totalDrag.y)
                        if (max(absX, absY) < SWIPE_THRESHOLD) return@detectDragGestures
                        if (absX > absY) {
                            if (totalDrag.x < 0) {
                                swipeLeftApp?.let(viewModel::launch)
                                    ?: Toast.makeText(context, "No swipe-left app set — long-press an app in the drawer", Toast.LENGTH_SHORT).show()
                            } else {
                                swipeRightApp?.let(viewModel::launch)
                                    ?: Toast.makeText(context, "No swipe-right app set — long-press an app in the drawer", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (totalDrag.y < 0) {
                                onOpenDrawer()
                            } else {
                                if (!expandNotifications()) toastAccessibilityNeeded(context, "open notifications")
                            }
                        }
                    },
                )
            }
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = if (alignment == HomeAlignment.CENTER) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(text = now.format(timeFormatter), style = MaterialTheme.typography.displayMedium, color = onBackground)
        Text(text = now.format(dateFormatter), style = MaterialTheme.typography.bodyLarge, color = onBackground)
        Text(text = "$batteryPercent%", style = MaterialTheme.typography.bodyMedium, color = onBackground)

        if (showScreenTime) {
            Text(
                text = screenTimeMillis?.let { "Screen time today: ${formatScreenTime(it)}" } ?: "Screen time: grant Usage access",
                style = MaterialTheme.typography.bodyMedium,
                color = onBackground.copy(alpha = 0.7f),
                modifier = Modifier.clickable(onClick = onOpenUsage),
            )
        }

        Spacer(modifier = Modifier.padding(top = 32.dp))

        FavoritesList(
            favorites = favorites,
            alignment = alignment,
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

        Text(
            text = "Settings",
            color = onBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .clickable(onClick = onOpenSettings)
                .padding(vertical = 8.dp),
        )

        if (!accessibilityEnabled) {
            Text(
                text = "Enable Accessibility for lock & notification gestures",
                color = onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clickable { context.startActivity(accessibilitySettingsIntent()) }
                    .padding(top = 4.dp),
            )
        }
    }
}

private fun formatScreenTime(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun toastAccessibilityNeeded(context: Context, action: String) {
    Toast.makeText(
        context,
        "Enable Quiet's Accessibility service in Settings to $action",
        Toast.LENGTH_SHORT,
    ).show()
}

@Composable
private fun FavoritesList(
    favorites: List<LaunchableApp>,
    alignment: HomeAlignment,
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
                    textAlign = if (alignment == HomeAlignment.CENTER) TextAlign.Center else TextAlign.Start,
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
