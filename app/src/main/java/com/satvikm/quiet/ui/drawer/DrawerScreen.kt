package com.satvikm.quiet.ui.drawer

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.R
import com.satvikm.quiet.data.settings.GestureSlot
import com.satvikm.quiet.domain.model.LaunchableApp
import com.satvikm.quiet.util.appInfoIntent
import com.satvikm.quiet.util.requestUninstall

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(
    viewModel: DrawerViewModel = hiltViewModel(),
    pickForGesture: GestureSlot? = null,
    onGesturePicked: () -> Unit = {},
) {
    val context = LocalContext.current
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val workFavoriteIds by viewModel.workFavoriteIds.collectAsStateWithLifecycle()
    val workAllowedIds by viewModel.workAllowedIds.collectAsStateWithLifecycle()
    val blockedPackageNames by viewModel.blockedPackageNames.collectAsStateWithLifecycle()
    val mutedPackageNames by viewModel.mutedPackageNames.collectAsStateWithLifecycle()
    val onBackground = MaterialTheme.colorScheme.onBackground
    val removalCooldownMessage = stringResource(R.string.removal_cooldown_toast)

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var menuTarget by remember { mutableStateOf<LaunchableApp?>(null) }
    var renameTarget by remember { mutableStateOf<LaunchableApp?>(null) }

    LaunchedEffect(Unit) {
        viewModel.onQueryChange("")
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        if (pickForGesture != null) {
            Text(
                text = stringResource(
                    if (pickForGesture == GestureSlot.SWIPE_LEFT) R.string.choose_app_for_swipe_left else R.string.choose_app_for_swipe_right,
                ),
                color = onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        BasicTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            textStyle = TextStyle(color = onBackground, fontSize = MaterialTheme.typography.titleLarge.fontSize),
            cursorBrush = SolidColor(onBackground),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    apps.firstOrNull()?.let { app ->
                        if (pickForGesture != null) {
                            viewModel.setGestureApp(pickForGesture, app)
                            onGesturePicked()
                        } else {
                            viewModel.launch(app)
                        }
                    }
                },
            ),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        color = onBackground.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                innerTextField()
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps, key = { it.id }) { app: LaunchableApp ->
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (pickForGesture != null) {
                                        viewModel.setGestureApp(pickForGesture, app)
                                        onGesturePicked()
                                    } else {
                                        viewModel.launch(app)
                                    }
                                },
                                onLongClick = { if (pickForGesture == null) menuTarget = app },
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = app.displayLabel, color = onBackground)
                    }

                    DropdownMenu(
                        expanded = menuTarget?.id == app.id,
                        onDismissRequest = { menuTarget = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (app.id in favoriteIds) R.string.remove_from_favorites else R.string.pin_to_favorites)) },
                            onClick = {
                                viewModel.toggleFavorite(app)
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            onClick = {
                                menuTarget = null
                                renameTarget = app
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (app.isHidden) R.string.unhide else R.string.hide)) },
                            onClick = {
                                viewModel.setHidden(app, !app.isHidden)
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (app.id in workAllowedIds) R.string.remove_from_work_mode else R.string.add_to_work_mode)) },
                            onClick = {
                                viewModel.toggleWorkAllowed(app)
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (app.id in workFavoriteIds) R.string.unpin_from_work_mode_favorites else R.string.pin_to_work_mode_favorites,
                                    ),
                                )
                            },
                            onClick = {
                                viewModel.toggleWorkFavorite(app)
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.set_as_swipe_left_app)) },
                            onClick = {
                                viewModel.setGestureApp(GestureSlot.SWIPE_LEFT, app)
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.set_as_swipe_right_app)) },
                            onClick = {
                                viewModel.setGestureApp(GestureSlot.SWIPE_RIGHT, app)
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (app.packageName in blockedPackageNames) R.string.remove_friction else R.string.add_friction)) },
                            onClick = {
                                val wasBlocked = app.packageName in blockedPackageNames
                                viewModel.setBlocked(app, !wasBlocked)
                                if (wasBlocked) Toast.makeText(context, removalCooldownMessage, Toast.LENGTH_LONG).show()
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (app.packageName in mutedPackageNames) R.string.unmute_notifications else R.string.mute_notifications)) },
                            onClick = {
                                viewModel.setMuted(app, app.packageName !in mutedPackageNames)
                                menuTarget = null
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.app_info)) },
                            onClick = {
                                menuTarget = null
                                context.startActivity(appInfoIntent(app.packageName))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.uninstall)) },
                            onClick = {
                                menuTarget = null
                                requestUninstall(context, app.packageName)
                            },
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { app ->
        RenameDialog(
            app = app,
            onConfirm = { newLabel ->
                viewModel.rename(app, newLabel)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

@Composable
private fun RenameDialog(app: LaunchableApp, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by remember(app.id) { mutableStateOf(app.displayLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename)) },
        text = {
            TextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.reset)) }
        },
    )
}
