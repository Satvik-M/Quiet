package com.satvikm.quiet.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.satvikm.quiet.util.accessibilitySettingsIntent
import com.satvikm.quiet.util.createChangeDefaultLauncherIntent
import com.satvikm.quiet.util.isDefaultLauncher
import com.satvikm.quiet.util.isGestureAccessibilityServiceEnabled
import com.satvikm.quiet.util.isNotificationAccessGranted
import com.satvikm.quiet.util.isPostNotificationsGranted
import com.satvikm.quiet.util.notificationListenerSettingsIntent
import com.satvikm.quiet.util.usageAccessSettingsIntent

private const val STEP_COUNT = 4

@Composable
fun OnboardingScreen(onFinish: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val onBackground = MaterialTheme.colorScheme.onBackground
    var step by remember { mutableIntStateOf(0) }

    var isDefaultLauncherState by remember { mutableStateOf(isDefaultLauncher(context)) }
    var accessibilityGranted by remember { mutableStateOf(isGestureAccessibilityServiceEnabled(context)) }
    var notificationAccessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var postNotificationsGranted by remember { mutableStateOf(isPostNotificationsGranted(context)) }
    val usageAccessGranted by viewModel.usageAccessGranted.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultLauncherState = isDefaultLauncher(context)
                accessibilityGranted = isGestureAccessibilityServiceEnabled(context)
                notificationAccessGranted = isNotificationAccessGranted(context)
                postNotificationsGranted = isPostNotificationsGranted(context)
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestDefaultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        isDefaultLauncherState = isDefaultLauncher(context)
    }
    val requestPostNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> postNotificationsGranted = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            when (step) {
                0 -> {
                    Text(text = stringResource(R.string.onboarding_welcome_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(R.string.onboarding_welcome_body),
                        color = onBackground.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                1 -> {
                    Text(text = stringResource(R.string.onboarding_default_launcher_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(R.string.onboarding_default_launcher_body),
                        color = onBackground.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                    )
                    Text(
                        text = if (isDefaultLauncherState) stringResource(R.string.onboarding_default_launcher_done) else stringResource(R.string.set_as_default),
                        color = onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable(enabled = !isDefaultLauncherState) {
                                requestDefaultLauncher.launch(createChangeDefaultLauncherIntent(context))
                            }
                            .padding(vertical = 8.dp),
                    )
                }
                2 -> {
                    Text(text = stringResource(R.string.onboarding_permissions_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(R.string.onboarding_permissions_body),
                        color = onBackground.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                    )
                    PermissionRow(
                        label = stringResource(R.string.onboarding_usage_access),
                        granted = usageAccessGranted,
                        onClick = { context.startActivity(usageAccessSettingsIntent()) },
                    )
                    PermissionRow(
                        label = stringResource(R.string.onboarding_accessibility),
                        granted = accessibilityGranted,
                        onClick = { context.startActivity(accessibilitySettingsIntent()) },
                    )
                    PermissionRow(
                        label = stringResource(R.string.onboarding_notification_access),
                        granted = notificationAccessGranted,
                        onClick = { context.startActivity(notificationListenerSettingsIntent()) },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            label = stringResource(R.string.onboarding_post_notifications),
                            granted = postNotificationsGranted,
                            onClick = { requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                    }
                }
                3 -> {
                    Text(text = stringResource(R.string.onboarding_done_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(R.string.onboarding_done_body),
                        color = onBackground.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (step < STEP_COUNT - 1) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable(onClick = onFinish),
                )
            } else {
                Text(text = "")
            }
            Text(
                text = if (step < STEP_COUNT - 1) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_get_started),
                color = onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable {
                    if (step < STEP_COUNT - 1) step++ else onFinish()
                },
            )
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = onBackground,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (granted) stringResource(R.string.granted_label) else stringResource(R.string.grant_action),
            color = onBackground.copy(alpha = if (granted) 0.4f else 0.7f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
