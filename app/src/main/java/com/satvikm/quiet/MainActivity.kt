package com.satvikm.quiet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.data.settings.AppFontFamily
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.data.settings.ThemeMode
import com.satvikm.quiet.ui.drawer.DrawerScreen
import com.satvikm.quiet.ui.home.HomeScreen
import com.satvikm.quiet.ui.settings.SettingsScreen
import com.satvikm.quiet.ui.settings.SettingsViewModel
import com.satvikm.quiet.ui.theme.QuietTheme
import com.satvikm.quiet.ui.usage.UsageScreen
import com.satvikm.quiet.util.createChangeDefaultLauncherIntent
import com.satvikm.quiet.util.isDefaultLauncher
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // MainActivity is singleTask, so a subsequent Home press while it's
    // already running redelivers the HOME intent via onNewIntent rather
    // than recreating the activity. Bump this so the composition can react
    // (close the drawer/settings) the same way BackHandler does.
    private val homePressCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val fontFamilySetting by settingsViewModel.fontFamily.collectAsStateWithLifecycle()
            val fontSizeSetting by settingsViewModel.fontSize.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            val fontFamily = when (fontFamilySetting) {
                AppFontFamily.SANS -> FontFamily.SansSerif
                AppFontFamily.MONOSPACE -> FontFamily.Monospace
            }

            QuietTheme(darkTheme = darkTheme, fontFamily = fontFamily, fontScale = fontSizeSetting.scale) {
                AppRoot(homePressCount = homePressCount.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePressCount.intValue++
        }
    }
}

private enum class Route { HOME, DRAWER, SETTINGS, USAGE }

@Composable
private fun AppRoot(homePressCount: Int) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDefault by remember { mutableStateOf(isDefaultLauncher(context)) }
    var route by remember { mutableStateOf(Route.HOME) }
    var gesturePickSlot by remember { mutableStateOf<GestureSlot?>(null) }
    // Usage can be opened from either Home or Settings; remember which so
    // back returns to the right place instead of always going Home.
    var usageReturnRoute by remember { mutableStateOf(Route.HOME) }

    // The home screen is the root of the task; there's nothing beneath it
    // to pop back to, except the drawer/settings/picker the user may have
    // opened.
    BackHandler(enabled = true) {
        when {
            gesturePickSlot != null -> gesturePickSlot = null
            route == Route.USAGE -> route = usageReturnRoute
            route != Route.HOME -> route = Route.HOME
        }
    }

    LaunchedEffect(homePressCount) {
        if (homePressCount > 0) {
            route = Route.HOME
            gesturePickSlot = null
        }
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
            .background(MaterialTheme.colorScheme.background),
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
        } else if (gesturePickSlot != null) {
            DrawerScreen(
                pickForGesture = gesturePickSlot,
                onGesturePicked = { gesturePickSlot = null },
            )
        } else when (route) {
            Route.DRAWER -> DrawerScreen()
            Route.USAGE -> UsageScreen(onBack = { route = usageReturnRoute })
            Route.SETTINGS -> SettingsScreen(
                onBack = { route = Route.HOME },
                onPickGestureApp = { slot -> gesturePickSlot = slot },
                onOpenUsage = {
                    usageReturnRoute = Route.SETTINGS
                    route = Route.USAGE
                },
            )
            Route.HOME -> HomeScreen(
                onOpenDrawer = { route = Route.DRAWER },
                onOpenSettings = { route = Route.SETTINGS },
                onOpenUsage = {
                    usageReturnRoute = Route.HOME
                    route = Route.USAGE
                },
            )
        }
    }
}
