package com.satvikm.quiet

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.ui.home.HomeViewModel
import com.satvikm.quiet.ui.theme.QuietBlack
import com.satvikm.quiet.ui.theme.QuietTheme
import com.satvikm.quiet.util.createChangeDefaultLauncherIntent
import com.satvikm.quiet.util.isDefaultLauncher
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuietTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDefault by remember { mutableStateOf(isDefaultLauncher(context)) }
    var showAllApps by remember { mutableStateOf(false) }

    // The home screen is the root of the task; there's nothing beneath it
    // to pop back to, except the all-apps screen the user may have opened.
    BackHandler(enabled = true) {
        if (showAllApps) showAllApps = false
    }

    // The user sets the default launcher in a system Settings screen, not
    // in this app, so re-check whenever we come back to the foreground.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefault = isDefaultLauncher(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestDefaultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Never trust the result code alone; re-resolve who holds HOME.
        isDefault = isDefaultLauncher(context)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(QuietBlack),
    ) {
        if (!isDefault) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Quiet isn't your home screen yet.",
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Button(
                    onClick = {
                        requestDefaultLauncher.launch(createChangeDefaultLauncherIntent(context))
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Set as default")
                }
            }
        } else if (showAllApps) {
            AllAppsScreen(onBack = { showAllApps = false })
        } else {
            HomeSurface(onOpenAllApps = { showAllApps = true })
        }
    }
}

@Composable
private fun HomeSurface(onOpenAllApps: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
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
                .clickable(onClick = onOpenAllApps)
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun AllAppsScreen(onBack: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            text = "‹ Home",
            color = onBackground,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(24.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps, key = { it.id }) { app: LaunchableApp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.launch(app) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = app.label,
                        color = onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (app.id in favoriteIds) "★" else "☆",
                        color = onBackground,
                        modifier = Modifier.clickable { viewModel.toggleFavorite(app) },
                    )
                }
            }
        }
    }
}
