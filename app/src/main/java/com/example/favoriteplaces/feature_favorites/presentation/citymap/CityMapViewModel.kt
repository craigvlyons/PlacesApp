package com.example.favoriteplaces.feature_favorites.presentation.citymap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class CityMapViewModel @Inject constructor(
    private val favoriteUseCases: FavoriteUseCases,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(CityMapUiState())
    val state: StateFlow<CityMapUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()
    private val favoriteJobs = mutableMapOf<Int, Job>()

    init {
        val favoriteId = savedStateHandle.get<Int>(Screen.CityMapScreen.FAVORITE_ID)
        val city = savedStateHandle.get<String>(Screen.CityMapScreen.CITY)
        val color = savedStateHandle.get<Int>(Screen.CityMapScreen.COLOR)

        when {
            favoriteId != null && favoriteId >= 0 -> loadFavorite(favoriteId)
            !city.isNullOrBlank() && color != null -> observeGroup(city, color)
            else -> _state.value = CityMapUiState(
                isLoading = false,
                message = "This map link is incomplete.",
            )
        }
    }

    private fun loadFavorite(favoriteId: Int) {
        viewModelScope.launch {
            try {
                val favorite = favoriteUseCases.getFavorite(favoriteId)
                _state.value = if (favorite == null) {
                    CityMapUiState(
                        isLoading = false,
                        message = "This saved place is no longer available.",
                    )
                } else {
                    mapState(title = favorite.title, favorites = listOf(favorite))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to load a saved-place map", exception)
                _state.value = CityMapUiState(
                    isLoading = false,
                    message = "The saved place could not be loaded.",
                )
            }
        }
    }

    private fun observeGroup(city: String, color: Int) {
        viewModelScope.launch {
            try {
                favoriteUseCases.getFavoritesByCityAndColor(city, color).collect { favorites ->
                    _state.value = mapState(title = city, favorites = favorites)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to load a saved-place map group", exception)
                _state.value = CityMapUiState(
                    title = city,
                    isLoading = false,
                    message = "Saved places could not be loaded.",
                )
            }
        }
    }

    private fun mapState(title: String, favorites: List<Favorite>): CityMapUiState {
        val validPlaces = favorites.filter { it.hasValidCoordinates() }
        val message = when {
            favorites.isEmpty() -> "No saved places match this map."
            validPlaces.isEmpty() -> "These saved places do not have valid map coordinates."
            else -> null
        }
        return CityMapUiState(
            title = title,
            places = validPlaces,
            isLoading = false,
            message = message,
            coordinateWarning = if (validPlaces.isNotEmpty() && validPlaces.size < favorites.size) {
                "Some saved places could not be shown because their coordinates are invalid."
            } else {
                null
            },
        )
    }

    private fun Favorite.hasValidCoordinates(): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    fun updateFavoriteHeart(id: Int, selected: Boolean) {
        if (favoriteJobs[id]?.isActive == true) return
        val previous = _state.value.places.firstOrNull { it.id == id }?.isFavorite ?: return
        if (previous == selected) return
        _state.value = _state.value.copy(
            places = _state.value.places.map { place ->
                if (place.id == id) place.copy(isFavorite = selected) else place
            }
        )
        favoriteJobs[id] = viewModelScope.launch {
            try {
                favoriteUseCases.updateIsFavorite(id, selected)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to update map Favorite heart", exception)
                _state.value = _state.value.copy(
                    places = _state.value.places.map { place ->
                        if (place.id == id) place.copy(isFavorite = previous) else place
                    }
                )
                _events.emit(UiEvent.Message("The Favorite heart could not be updated."))
            } finally {
                favoriteJobs.remove(id)
            }
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch { _events.emit(UiEvent.Message(message)) }
    }

    sealed interface UiEvent {
        data class Message(val value: String) : UiEvent
    }

    private companion object {
        const val TAG = "CityMapVM"
    }
}
