package com.satvikm.quiet.ui.friction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.satvikm.quiet.data.block.BlocklistRepository
import kotlinx.coroutines.delay

@Composable
fun FrictionScreen(
    packageName: String,
    label: String,
    blocklistRepository: BlocklistRepository,
    onClose: () -> Unit,
    onContinue: () -> Unit,
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    var loaded by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var dailyLimitReached by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    LaunchedEffect(packageName) {
        val entity = blocklistRepository.get(packageName)
        secondsLeft = entity?.delaySeconds ?: 0
        dailyLimitReached = entity?.let { !blocklistRepository.canContinue(it) } ?: false
        loaded = true
    }

    LaunchedEffect(secondsLeft, loaded) {
        if (loaded && secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
    }

    if (!loaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineMedium, color = onBackground)

        Spacer(modifier = Modifier.height(24.dp))

        when {
            dailyLimitReached -> Text(
                text = "Daily limit reached",
                color = onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
            )
            secondsLeft > 0 -> Text(
                text = "$secondsLeft",
                style = MaterialTheme.typography.displayLarge,
                color = onBackground,
            )
            else -> Text(
                text = "Take a breath.",
                color = onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Close",
            style = MaterialTheme.typography.titleLarge,
            color = onBackground,
            modifier = Modifier
                .clickable(onClick = onClose)
                .padding(16.dp),
        )

        if (!dailyLimitReached && secondsLeft == 0) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleLarge,
                color = onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable(onClick = onContinue)
                    .padding(16.dp),
            )
        }
    }
}
