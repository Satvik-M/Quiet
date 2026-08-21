package com.satvikm.quiet.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satvikm.quiet.data.backup.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BackupResult { EXPORT_SUCCESS, IMPORT_SUCCESS, ERROR }

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _results = MutableSharedFlow<BackupResult>(extraBufferCapacity = 1)
    val results: SharedFlow<BackupResult> = _results

    fun export(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching { backupRepository.exportTo(uri) }
                .fold({ BackupResult.EXPORT_SUCCESS }, { BackupResult.ERROR })
            _results.emit(result)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching { backupRepository.importFrom(uri) }
                .fold({ BackupResult.IMPORT_SUCCESS }, { BackupResult.ERROR })
            _results.emit(result)
        }
    }
}
