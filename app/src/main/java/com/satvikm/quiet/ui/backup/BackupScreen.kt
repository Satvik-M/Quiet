package com.satvikm.quiet.ui.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satvikm.quiet.R

@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val onBackground = MaterialTheme.colorScheme.onBackground

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }

    val exportSuccessMessage = stringResource(R.string.backup_export_success)
    val importSuccessMessage = stringResource(R.string.backup_import_success)
    val errorMessage = stringResource(R.string.backup_error)
    LaunchedEffect(Unit) {
        viewModel.results.collect { result ->
            val message = when (result) {
                BackupResult.EXPORT_SUCCESS -> exportSuccessMessage
                BackupResult.IMPORT_SUCCESS -> importSuccessMessage
                BackupResult.ERROR -> errorMessage
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.back),
            color = onBackground,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 24.dp),
        )

        Text(text = stringResource(R.string.backup_title), color = onBackground, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.backup_hint),
            color = onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        Text(
            text = stringResource(R.string.export_settings),
            color = onBackground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable { exportLauncher.launch("quiet-backup.json") }
                .padding(vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.import_settings),
            color = onBackground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable { importLauncher.launch(arrayOf("application/json")) }
                .padding(vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.import_replaces_hint),
            color = onBackground.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
