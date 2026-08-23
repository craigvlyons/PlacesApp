package com.example.favoriteplaces.feature_favorites.presentation.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.data.backup.FavoriteBackupStore
import com.example.favoriteplaces.feature_favorites.data.backup.VerifiedFavoriteBackup
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DataBackupViewModel @Inject constructor(
    private val backupStore: FavoriteBackupStore
) : ViewModel() {
    private val _state = MutableStateFlow(DataBackupUiState())
    val state: StateFlow<DataBackupUiState> = _state.asStateFlow()

    private var pendingBackup: VerifiedFavoriteBackup? = null

    fun export(uri: Uri) {
        runOperation {
            val result = backupStore.exportTo(uri)
            _state.value = _state.value.copy(
                message = "Backup saved with ${result.recordCount} places.",
                error = null
            )
        }
    }

    fun inspectImport(uri: Uri) {
        pendingBackup = null
        _state.value = _state.value.copy(pendingImportCount = null)
        runOperation {
            val backup = backupStore.inspect(uri)
            pendingBackup = backup
            _state.value = _state.value.copy(
                pendingImportCount = backup.recordCount,
                message = null,
                error = null
            )
        }
    }

    fun confirmImport() {
        val backup = pendingBackup ?: return
        runOperation {
            val result = backupStore.import(backup)
            pendingBackup = null
            _state.value = _state.value.copy(
                pendingImportCount = null,
                message = when {
                    result.insertedCount > 0 ->
                        "Imported ${result.insertedCount} places. Added missing Google details to ${result.enrichedCount} places."
                    result.enrichedCount > 0 ->
                        "Added missing Google details to ${result.enrichedCount} places. Existing place details were kept."
                    else -> "This backup is already imported. No places changed."
                },
                error = null
            )
        }
    }

    fun cancelImport() {
        pendingBackup = null
        _state.value = _state.value.copy(pendingImportCount = null)
    }

    fun clearNotice() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    private fun runOperation(operation: suspend () -> Unit) {
        if (_state.value.isWorking) return
        _state.value = _state.value.copy(isWorking = true, message = null, error = null)
        viewModelScope.launch {
            try {
                operation()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Backup operation failed", exception)
                _state.value = _state.value.copy(
                    error = exception.toUserMessage(),
                    message = null
                )
            } finally {
                _state.value = _state.value.copy(isWorking = false)
            }
        }
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is com.example.favoriteplaces.feature_favorites.data.backup.InvalidFavoriteBackupException ->
            message ?: "That file is not a valid Places backup."
        is com.example.favoriteplaces.feature_favorites.data.backup.FavoriteBackupConflictException ->
            "Import stopped because existing places differ from this backup. Nothing was changed."
        else -> "The backup operation could not be completed. Nothing was deleted."
    }

    private companion object {
        const val TAG = "DataBackup"
    }
}

data class DataBackupUiState(
    val isWorking: Boolean = false,
    val pendingImportCount: Int? = null,
    val message: String? = null,
    val error: String? = null
)
