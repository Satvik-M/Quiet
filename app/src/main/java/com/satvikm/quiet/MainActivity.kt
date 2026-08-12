package com.satvikm.quiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // The home screen has nothing to pop back to; consume
                    // the event so the launcher never finishes itself.
                }
            },
        )

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
        } else {
            // A debug listing for M3; the real home surface with favorites
            // and gestures lands in M4/M5.
            DebugAppList()
        }
    }
}

@Composable
private fun DebugAppList(viewModel: HomeViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(apps, key = { "${it.componentName}/${it.userHandle}" }) { app: LaunchableApp ->
            Text(
                text = app.label,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.launch(app) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}
