package com.satvikm.quiet.ui.drawer

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.domain.model.LaunchableApp

@Composable
fun DrawerScreen(viewModel: DrawerViewModel = hiltViewModel()) {
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val onBackground = MaterialTheme.colorScheme.onBackground

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        BasicTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            textStyle = TextStyle(color = onBackground, fontSize = MaterialTheme.typography.titleLarge.fontSize),
            cursorBrush = SolidColor(onBackground),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = { apps.firstOrNull()?.let { viewModel.launch(it) } },
            ),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search",
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
