package com.satvikm.quiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
        }
    }
}
