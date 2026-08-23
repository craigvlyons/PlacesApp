package com.example.favoriteplaces.feature_favorites.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.feature_favorites.domain.use_case.settings.NearbyTypePickerUpdateResult
import com.example.favoriteplaces.feature_favorites.domain.use_case.settings.UpdateNearbyTypePickerUseCase
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: SavedPlacesPreferencesRepository,
    private val updateNearbyTypePicker: UpdateNearbyTypePickerUseCase,
) : ViewModel() {
    val preferences = preferencesRepository.preferences
        .catch { exception ->
            if (exception is CancellationException) throw exception
            PrivacySafeLog.error(TAG, "Failed to observe settings preferences", exception)
            emit(SavedPlacesPreferences())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SavedPlacesPreferences(),
        )
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()
    private var appColorJob: Job? = null
    private var cardColorJob: Job? = null

    fun setAppColor(color: Int) {
        appColorJob?.cancel()
        appColorJob = updatePreference(
            operation = "save app color",
            failureMessage = "The app color could not be saved.",
        ) { preferencesRepository.setAppColor(color) }
    }

    fun setDefaultCardColor(color: Int) {
        cardColorJob?.cancel()
        cardColorJob = updatePreference(
            operation = "save new-place color",
            failureMessage = "The default place color could not be saved.",
        ) { preferencesRepository.setDefaultCardColor(color) }
    }

    fun addNearbyType(storageId: String) = updateNearbyType(storageId, add = true)

    fun removeNearbyType(storageId: String) = updateNearbyType(storageId, add = false)

    private fun updateNearbyType(storageId: String, add: Boolean) {
        viewModelScope.launch {
            try {
                when (updateNearbyTypePicker(storageId, add)) {
                    NearbyTypePickerUpdateResult.LimitReached -> _messages.emit(
                        "Remove a place type before adding another."
                    )
                    NearbyTypePickerUpdateResult.Unsupported ->
                        _messages.emit("That place type is not supported.")
                    NearbyTypePickerUpdateResult.Updated,
                    NearbyTypePickerUpdateResult.Unchanged -> Unit
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to update new-place type picker", exception)
                _messages.emit("Place-type choices could not be updated.")
            }
        }
    }

    private fun updatePreference(
        operation: String,
        failureMessage: String,
        update: suspend () -> Unit,
    ): Job = viewModelScope.launch {
        try {
            update()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            PrivacySafeLog.error(TAG, "Failed to $operation", exception)
            _messages.emit(failureMessage)
        }
    }

    private companion object {
        const val TAG = "SettingsVM"
    }
}
