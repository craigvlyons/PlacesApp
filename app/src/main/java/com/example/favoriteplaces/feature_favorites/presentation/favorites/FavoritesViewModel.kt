package com.example.favoriteplaces.feature_favorites.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteDetailsRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadataRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadataResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FavoritesUiEvent {
    data class ShowSnackbar(val message: String) : FavoritesUiEvent
    data class PlaceDeleted(val favorite: Favorite) : FavoritesUiEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModel @Inject constructor(
    private val favoriteUseCases: FavoriteUseCases,
    private val preferencesRepository: SavedPlacesPreferencesRepository,
    private val detailsRepository: FavoriteDetailsRepository,
    private val actionMetadataRepository: SavedPlaceActionMetadataRepository,
) : ViewModel() {
    private val filters = MutableStateFlow(SavedPlacesFilters())
    private val loadGeneration = MutableStateFlow(0)
    private val _eventFlow = MutableSharedFlow<FavoritesUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    private val actionLoadingIds = MutableStateFlow<Set<Int>>(emptySet())

    private val loadedData = loadGeneration.flatMapLatest {
        combine(
            favoriteUseCases.getFavorites.observe(),
            preferencesRepository.preferences,
        ) { favorites, preferences -> FavoritesData(favorites, preferences) }
            .map<FavoritesData, FavoritesLoadResult>(FavoritesLoadResult::Loaded)
            .catch { exception ->
                if (exception is CancellationException) throw exception
                PrivacySafeLog.error(TAG, "Failed to observe saved places", exception)
                _eventFlow.emit(FavoritesUiEvent.ShowSnackbar("Saved places could not be loaded."))
                emit(FavoritesLoadResult.Failed)
            }
    }

    val state: StateFlow<FavoritesUiState> = combine(
        loadedData,
        filters,
        actionLoadingIds,
    ) { result, activeFilters, loadingIds ->
        when (result) {
            FavoritesLoadResult.Failed -> FavoritesUiState(
                isLoading = false,
                filters = activeFilters,
                errorMessage = "Saved places could not be loaded.",
                loadingActionIds = loadingIds,
            )
            is FavoritesLoadResult.Loaded -> result.data.toUiState(activeFilters).copy(
                loadingActionIds = loadingIds,
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FavoritesUiState(),
        )

    private val placeMutationJobs = mutableMapOf<Int, kotlinx.coroutines.Job>()
    private val actionMetadataJobs = mutableMapOf<Int, kotlinx.coroutines.Job>()
    private val actionMetadataLoadedIds = mutableSetOf<Int>()
    private var orderJob: kotlinx.coroutines.Job? = null
    private var viewModeJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.Order -> updateOrder(event.favoriteOrder)
            is FavoritesEvent.DeleteFavorite -> deleteFavorite(event.favorite)
            is FavoritesEvent.RestoreFavorite -> restoreFavorite(event.favorite)
            is FavoritesEvent.LovedFavorite -> updateFavoriteHeart(event.id, event.isFavorite)
            is FavoritesEvent.ToggleListOrCardView -> updateViewMode(!state.value.isListView)
            is FavoritesEvent.ApplyFilters -> filters.value = event.filters
            FavoritesEvent.ClearFilters -> filters.value = SavedPlacesFilters()
            FavoritesEvent.RetryLoading -> loadGeneration.value += 1
        }
    }

    fun loadPlaceActionsIfNeeded(favorite: Favorite) {
        val id = favorite.id ?: return
        if (
            (favorite.phoneNumber != null && favorite.googlePrimaryType != null) ||
            id in actionMetadataLoadedIds ||
            actionMetadataJobs[id]?.isActive == true
        ) {
            return
        }
        val placeId = favorite.placeId?.trim()?.takeIf(String::isNotEmpty)
        if (placeId == null) {
            viewModelScope.launch {
                _eventFlow.emit(
                    FavoritesUiEvent.ShowSnackbar(
                        "This place is not connected to a Google listing."
                    )
                )
            }
            return
        }

        actionMetadataJobs[id] = viewModelScope.launch {
            actionLoadingIds.value += id
            try {
                when (val result = actionMetadataRepository.fetch(placeId)) {
                    is SavedPlaceActionMetadataResult.Failure -> {
                        _eventFlow.emit(
                            FavoritesUiEvent.ShowSnackbar(result.reason.toActionLookupMessage())
                        )
                    }
                    is SavedPlaceActionMetadataResult.Success -> {
                        detailsRepository.fillMissingGoogleMetadata(
                            id = id,
                            phoneNumber = result.metadata.phoneNumber,
                            googlePrimaryType = result.metadata.googlePrimaryType,
                        )
                        actionMetadataLoadedIds += id
                        val phone = favorite.phoneNumber ?: result.metadata.phoneNumber
                        val type = favorite.googlePrimaryType ?: result.metadata.googlePrimaryType
                        if (
                            phone.isNullOrBlank() &&
                            !com.example.favoriteplaces.feature_favorites.domain.model.places
                                .isReservationEligibleGoogleType(type)
                        ) {
                            _eventFlow.emit(
                                FavoritesUiEvent.ShowSnackbar(
                                    "Google does not list call or reservation details for this place."
                                )
                            )
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to save on-demand place actions", exception)
                _eventFlow.emit(
                    FavoritesUiEvent.ShowSnackbar(
                        "Call and reservation details could not be loaded. Please try again."
                    )
                )
            } finally {
                actionLoadingIds.value -= id
                actionMetadataJobs.remove(id)
            }
        }
    }

    private fun FavoritesData.toUiState(activeFilters: SavedPlacesFilters): FavoritesUiState {
        val sortedAll = favoriteUseCases.getFavorites.sort(
            favorites = favorites,
            favoriteOrder = preferences.favoriteOrder,
        )
        val sorted = sortedAll.filter { favorite ->
            (activeFilters.city == null || favorite.city == activeFilters.city) &&
                (activeFilters.color == null || favorite.color == activeFilters.color) &&
                (!activeFilters.favoriteOnly || favorite.isFavorite) &&
                favorite.matches(activeFilters.placeType)
        }
        return FavoritesUiState(
            favorites = sorted,
            favoriteOrder = preferences.favoriteOrder,
            cityColorList = sorted.toCityColorGroups(),
            isListView = preferences.isListView,
            isLoading = false,
            filters = activeFilters,
            availableCities = sortedAll.map(Favorite::city).distinct(),
            availableColors = sortedAll.map(Favorite::color).distinct(),
            availablePlaceTypes = sortedAll.mapNotNull(Favorite::placeType)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy(String::lowercase)
                .sortedWith(String.CASE_INSENSITIVE_ORDER),
            hasPlacesWithoutType = sortedAll.any { it.placeType.isNullOrBlank() },
        )
    }

    private fun updateOrder(favoriteOrder: FavoriteOrder) {
        if (state.value.favoriteOrder == favoriteOrder) return
        orderJob?.cancel()
        orderJob = viewModelScope.launch {
            try {
                preferencesRepository.setFavoriteOrder(favoriteOrder)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to save place ordering", exception)
                _eventFlow.emit(FavoritesUiEvent.ShowSnackbar("The sort preference could not be saved."))
            }
        }
    }

    private fun updateViewMode(isListView: Boolean) {
        viewModeJob?.cancel()
        viewModeJob = viewModelScope.launch {
            try {
                preferencesRepository.setListView(isListView)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to save place view mode", exception)
                _eventFlow.emit(FavoritesUiEvent.ShowSnackbar("The view preference could not be saved."))
            }
        }
    }

    private fun deleteFavorite(favorite: Favorite) {
        val id = favorite.id ?: return
        if (placeMutationJobs[id]?.isActive == true) return
        placeMutationJobs[id] = viewModelScope.launch {
            try {
                favoriteUseCases.deleteFavorite(favorite)
                _eventFlow.emit(FavoritesUiEvent.PlaceDeleted(favorite))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to delete a place", exception)
                _eventFlow.emit(FavoritesUiEvent.ShowSnackbar("The place could not be deleted."))
            } finally {
                placeMutationJobs.remove(id)
            }
        }
    }

    private fun restoreFavorite(favorite: Favorite) {
        val id = favorite.id ?: return
        if (placeMutationJobs[id]?.isActive == true) return
        placeMutationJobs[id] = viewModelScope.launch {
            try {
                favoriteUseCases.addFavorite(favorite)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to restore a deleted place", exception)
                _eventFlow.emit(FavoritesUiEvent.ShowSnackbar("The place could not be restored."))
            } finally {
                placeMutationJobs.remove(id)
            }
        }
    }

    private fun updateFavoriteHeart(id: Int, isFavorite: Boolean) {
        if (placeMutationJobs[id]?.isActive == true) return
        placeMutationJobs[id] = viewModelScope.launch {
            try {
                favoriteUseCases.updateIsFavorite(id, isFavorite)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to update Favorite heart", exception)
                _eventFlow.emit(
                    FavoritesUiEvent.ShowSnackbar("The Favorite heart could not be updated.")
                )
            } finally {
                placeMutationJobs.remove(id)
            }
        }
    }

    private fun List<Favorite>.toCityColorGroups(): List<CityGroupUiModel> =
        groupBy(Favorite::city).map { (city, cityFavorites) ->
            CityGroupUiModel(
                city = city,
                colorVariations = cityFavorites.groupBy(Favorite::color).map { (color, items) ->
                    ColorGroupUiModel(color = color, favorites = items)
                },
            )
        }

    private fun Favorite.matches(filter: SavedPlaceTypeFilter?): Boolean = when (filter) {
        null -> true
        SavedPlaceTypeFilter.NotSet -> placeType.isNullOrBlank()
        is SavedPlaceTypeFilter.Exact -> placeType?.trim()?.equals(filter.value, ignoreCase = true) == true
    }

    private companion object {
        const val TAG = "FavoritesViewModel"
    }

    private data class FavoritesData(
        val favorites: List<Favorite>,
        val preferences: SavedPlacesPreferences,
    )

    private sealed interface FavoritesLoadResult {
        data class Loaded(val data: FavoritesData) : FavoritesLoadResult
        data object Failed : FavoritesLoadResult
    }
}

private fun PlaceSearchFailure.toActionLookupMessage(): String = when (category) {
    PlaceSearchFailureCategory.Authorization ->
        "Google could not authorize the call-details request. Check the Places API key settings."
    PlaceSearchFailureCategory.Quota ->
        "Google's request limit was reached. Please try again later."
    PlaceSearchFailureCategory.Network ->
        "Connect to the internet and try loading the call details again."
    PlaceSearchFailureCategory.NotFound ->
        "Google no longer recognizes this saved listing."
    PlaceSearchFailureCategory.InvalidRequest ->
        "This saved place is not connected to a valid Google listing."
    PlaceSearchFailureCategory.Unavailable,
    PlaceSearchFailureCategory.Unknown,
    -> "Call and reservation details are unavailable right now. Please try again."
}
