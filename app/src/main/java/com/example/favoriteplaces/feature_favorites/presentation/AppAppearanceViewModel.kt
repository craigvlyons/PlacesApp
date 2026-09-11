package com.example.favoriteplaces.feature_favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppAppearanceViewModel @Inject constructor(
    preferencesRepository: SavedPlacesPreferencesRepository,
) : ViewModel() {
    val preferences: StateFlow<SavedPlacesPreferences> = preferencesRepository.preferences
        .catch { exception ->
            if (exception is kotlinx.coroutines.CancellationException) throw exception
            PrivacySafeLog.error(TAG, "Failed to observe app appearance", exception)
            emit(SavedPlacesPreferences())
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SavedPlacesPreferences(),
    )

    private companion object {
        const val TAG = "AppAppearanceVM"
    }
}
