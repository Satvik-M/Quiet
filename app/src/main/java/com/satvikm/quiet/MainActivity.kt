package com.satvikm.quiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.satvikm.quiet.ui.theme.QuietBlack
import com.satvikm.quiet.ui.theme.QuietTheme
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
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(QuietBlack),
    ) {
        // Intentionally blank for M1; the real home surface lands in M4.
    }
}
