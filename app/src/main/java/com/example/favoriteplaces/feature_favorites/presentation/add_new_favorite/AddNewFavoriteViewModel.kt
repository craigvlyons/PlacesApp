package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.repository.CurrentLocationProvider
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchOrigin
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.SelectedPlace
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchSession
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceAlreadyExistsException
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.presentation.util.AddressResolver
import com.example.favoriteplaces.feature_favorites.presentation.util.PlaceCityResolver
import com.example.favoriteplaces.feature_favorites.presentation.util.restoreGeoCoordinates
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class AddNewFavoriteViewModel @Inject constructor(
    private val currentLocationProvider: CurrentLocationProvider,
    private val addressResolver: AddressResolver,
    private val placeSearchRepository: PlaceSearchRepository,
    private val favoriteUseCases: FavoriteUseCases,
    private val preferencesRepository: SavedPlacesPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val restoredQuery = savedStateHandle.get<String>(SEARCH_QUERY).orEmpty()
    private val restoredOrigin = savedStateHandle.restoreGeoCoordinates(
        ORIGIN_LATITUDE,
        ORIGIN_LONGITUDE,
        TAG,
    )
    private val _uiState = MutableStateFlow(
        AddNewFavoriteUiState(
            searchField = AddFavoriteTextFieldState(
                text = restoredQuery,
                hint = "Search for place...",
                isHintVisible = restoredQuery.isBlank(),
            ),
            searchOrigin = restoredOrigin,
            searchOriginLabel = savedStateHandle.get<String>(ORIGIN_LABEL)
                ?: if (restoredOrigin == null) "Location unavailable" else "Selected location",
        )
    )
    val uiState: StateFlow<AddNewFavoriteUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var searchJob: Job? = null
    private var selectionJob: Job? = null
    private var saveJob: Job? = null
    private var currentLocationJob: Job? = null
    private var activeSearchSession: PlaceSearchSession? = null
    private var activeOriginSession: PlaceSearchSession? = null
    private var originJob: Job? = null
    private var selectedPlace: SelectedPlace? = null
    private var deviceLocation: GeoCoordinates? = null

    init {
        getCurrentLocation()
    }

    fun onEvent(event: AddNewFavoriteEvent) {
        when (event) {
            is AddNewFavoriteEvent.EnteredSearch -> {
                savedStateHandle[SEARCH_QUERY] = event.value
                cancelPlaceSearch()
                _uiState.value = _uiState.value.copy(
                    searchField = _uiState.value.searchField.copy(text = event.value),
                    predictions = emptyList(),
                    selectedPlaceId = null,
                    isMapVisible = false,
                    hasSearched = false,
                    errorMessage = null,
                )
            }

            is AddNewFavoriteEvent.SelectedResult -> selectPrediction(event.predictionResult)
            AddNewFavoriteEvent.SaveFavorite -> saveSelectedPlace()
            AddNewFavoriteEvent.ToggleMapSelection -> toggleMapSelection()
            AddNewFavoriteEvent.Search -> searchPlaces(_uiState.value.searchField.text)
            is AddNewFavoriteEvent.EnteredOriginSearch -> {
                cancelOriginSearch()
                updateUiState(
                    originQuery = event.value,
                    originPredictions = emptyList(),
                    isOriginLoading = false,
                )
            }
            AddNewFavoriteEvent.SearchOrigin -> searchOrigins()
            is AddNewFavoriteEvent.SelectedOriginResult -> selectOrigin(event.predictionResult)
            is AddNewFavoriteEvent.SelectedOriginOnMap -> selectOriginOnMap(
                GeoCoordinates(event.latitude, event.longitude)
            )
            AddNewFavoriteEvent.UseDeviceLocation -> useDeviceLocation()
        }
    }

    private fun searchPlaces(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            emitMessage("Enter at least $MIN_QUERY_LENGTH characters to search.")
            return
        }

        cancelPlaceSearch()
        val session = placeSearchRepository.createSession()
        activeSearchSession = session

        searchJob = viewModelScope.launch {
            updateUiState(
                predictions = emptyList(),
                selectedPlaceId = null,
                thirdPartyAttributions = emptyList(),
                isMapVisible = false,
                isLoading = true,
                errorMessage = null,
            )
            try {
                when (val result = session.search(query, currentSearchOrigin())) {
                    is PlaceSearchResult.Success -> updateUiState(
                        predictions = result.value,
                        isLoading = false,
                        hasSearched = true,
                    )
                    is PlaceSearchResult.Failure -> {
                        updateUiState(
                            isLoading = false,
                            hasSearched = true,
                            errorMessage = result.reason.category.userMessage(),
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Place search failed unexpectedly", exception)
                if (activeSearchSession === session) {
                    updateUiState(
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = "Place search is unavailable. Please try again.",
                    )
                }
            } finally {
                if (activeSearchSession === session) {
                    updateUiState(isLoading = false)
                }
            }
        }
    }

    private fun selectPrediction(prediction: PlacePrediction) {
        if (selectionJob?.isActive == true) return
        val session = activeSearchSession
        if (session == null) {
            emitMessage("Search again before selecting a place.")
            return
        }

        selectionJob?.cancel()
        selectedPlace = null
        selectionJob = viewModelScope.launch {
            updateUiState(
                selectedPlaceId = prediction.placeId,
                isMapVisible = false,
                isLoading = true,
            )
            try {
                when (val result = session.select(prediction.placeId)) {
                    is PlaceSearchResult.Success -> {
                        val place = result.value
                        val coordinates = GeoCoordinates(place.latitude, place.longitude)
                        selectedPlace = place
                        updateUiState(
                            selectedPlaceId = place.placeId,
                            thirdPartyAttributions = place.thirdPartyAttributions,
                            mapCoordinates = coordinates,
                            isMapVisible = false,
                            isLoading = false,
                        )
                        activeSearchSession = null
                    }
                    is PlaceSearchResult.Failure -> {
                        activeSearchSession = null
                        updateUiState(
                            predictions = emptyList(),
                            selectedPlaceId = null,
                            thirdPartyAttributions = emptyList(),
                            isMapVisible = false,
                            isLoading = false,
                        )
                        _eventFlow.emit(UiEvent.ShowSnackbar(result.reason.category.userMessage()))
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Place selection failed unexpectedly", exception)
                if (activeSearchSession === session) {
                    activeSearchSession = null
                    selectedPlace = null
                    updateUiState(
                        predictions = emptyList(),
                        selectedPlaceId = null,
                        thirdPartyAttributions = emptyList(),
                        isMapVisible = false,
                    )
                    _eventFlow.emit(
                        UiEvent.ShowSnackbar("That place could not be selected. Please search again.")
                    )
                }
            } finally {
                if (selectedPlace?.placeId != prediction.placeId) {
                    updateUiState(isLoading = false)
                }
            }
        }
    }

    private fun saveSelectedPlace() {
        if (saveJob?.isActive == true) return
        saveJob = viewModelScope.launch {
            try {
                val place = selectedPlace
                    ?: throw SaveValidationException("Wait for the selected place to finish loading.")
                val city = PlaceCityResolver.resolve(place.addressComponents)
                    ?: throw SaveValidationException(
                        "Google did not provide a city for this place. Try another search result."
                    )
                val favorite = place.toFavorite(
                    city = city,
                    defaultColor = preferencesRepository.preferences.first().defaultCardColor,
                )
                favoriteUseCases.addFavorite(favorite)
                _eventFlow.emit(UiEvent.SaveFavorite)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SaveValidationException) {
                _eventFlow.emit(UiEvent.ShowSnackbar(exception.message.orEmpty()))
            } catch (_: SavedPlaceAlreadyExistsException) {
                _eventFlow.emit(UiEvent.ShowSnackbar("That place is already saved."))
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to save selected place", exception)
                _eventFlow.emit(
                    UiEvent.ShowSnackbar("The place could not be saved. Please try again.")
                )
            }
        }
    }

    private fun SelectedPlace.toFavorite(city: String, defaultColor: Int): Favorite = Favorite(
        id = null,
        placeId = placeId,
        title = displayName,
        address = formattedAddress,
        content = "",
        rating = 0,
        city = city,
        latitude = latitude,
        longitude = longitude,
        placeType = placeType,
        color = defaultColor,
        phoneNumber = phoneNumber,
        googlePrimaryType = googlePrimaryType,
    )

    private fun currentSearchOrigin(): PlaceSearchOrigin? {
        val origin = _uiState.value.searchOrigin ?: return null
        return PlaceSearchOrigin(
            latitude = origin.latitude,
            longitude = origin.longitude,
            radiusMeters = SEARCH_BIAS_RADIUS_METERS,
        )
    }

    private fun toggleMapSelection() {
        if (selectedPlace != null) {
            updateUiState(isMapVisible = !_uiState.value.isMapVisible)
        }
    }

    fun requestLocationPermission() {
        updateUiState(locationState = LocationPermissionState.RequestPermission)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            getCurrentLocation()
        } else {
            updateUiState(locationState = LocationPermissionState.Error)
        }
    }

    fun getCurrentLocation() {
        if (_uiState.value.locationState is LocationPermissionState.NoPermission) {
            requestLocationPermission()
            return
        }

        currentLocationJob?.cancel()
        currentLocationJob = viewModelScope.launch {
            updateUiState(locationState = LocationPermissionState.LocationLoading)
            try {
                val location = currentLocationProvider.currentLocation()
                if (location == null) {
                    updateUiState(locationState = LocationPermissionState.Error)
                    return@launch
                }
                updateUiState(
                    mapCoordinates = location,
                    locationState = LocationPermissionState.LocationAvailable(location),
                    searchOrigin = _uiState.value.searchOrigin ?: location,
                    searchOriginLabel = if (_uiState.value.searchOrigin == null) {
                        "Device location"
                    } else {
                        _uiState.value.searchOriginLabel
                    },
                )
                deviceLocation = location
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Current location request failed", exception)
                updateUiState(locationState = LocationPermissionState.Error)
            }
        }
    }

    private fun searchOrigins() {
        val query = _uiState.value.originQuery.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            emitMessage("Enter at least $MIN_QUERY_LENGTH characters to search for a location.")
            return
        }
        cancelOriginSearch()
        val session = placeSearchRepository.createSession()
        activeOriginSession = session
        originJob = viewModelScope.launch {
            updateUiState(isOriginLoading = true, originPredictions = emptyList())
            try {
                when (val result = session.search(query, currentSearchOrigin())) {
                    is PlaceSearchResult.Success -> updateUiState(
                        originPredictions = result.value,
                        isOriginLoading = false,
                    )
                    is PlaceSearchResult.Failure -> {
                        updateUiState(isOriginLoading = false)
                        _eventFlow.emit(UiEvent.ShowSnackbar(result.reason.category.userMessage()))
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Origin search failed unexpectedly", exception)
                if (activeOriginSession === session) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Locations could not be searched."))
                }
            } finally {
                if (activeOriginSession === session) {
                    updateUiState(isOriginLoading = false)
                }
            }
        }
    }

    private fun selectOrigin(prediction: PlacePrediction) {
        val session = activeOriginSession ?: return
        originJob?.cancel()
        originJob = viewModelScope.launch {
            updateUiState(isOriginLoading = true)
            try {
                when (val result = session.select(prediction.placeId)) {
                    is PlaceSearchResult.Success -> {
                        val place = result.value
                        setSearchOrigin(
                            GeoCoordinates(place.latitude, place.longitude),
                            place.displayName,
                        )
                    }
                    is PlaceSearchResult.Failure ->
                        _eventFlow.emit(UiEvent.ShowSnackbar(result.reason.category.userMessage()))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Origin selection failed unexpectedly", exception)
                if (activeOriginSession === session) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("That location could not be selected."))
                }
            } finally {
                if (activeOriginSession === session) {
                    activeOriginSession = null
                    updateUiState(isOriginLoading = false)
                }
            }
        }
    }

    private fun selectOriginOnMap(coordinates: GeoCoordinates) {
        cancelOriginSearch()
        originJob = viewModelScope.launch {
            updateUiState(isOriginLoading = true)
            val label = try {
                addressResolver.resolve(coordinates.latitude, coordinates.longitude)
                    ?: "Selected map location"
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Map origin address lookup failed", exception)
                "Selected map location"
            }
            setSearchOrigin(coordinates, label)
            updateUiState(isOriginLoading = false)
        }
    }

    private fun useDeviceLocation() {
        cancelOriginSearch()
        val location = deviceLocation
        if (location == null) {
            getCurrentLocation()
        } else {
            setSearchOrigin(location, "Device location")
        }
    }

    private fun setSearchOrigin(coordinates: GeoCoordinates, label: String) {
        savedStateHandle[ORIGIN_LATITUDE] = coordinates.latitude
        savedStateHandle[ORIGIN_LONGITUDE] = coordinates.longitude
        savedStateHandle[ORIGIN_LABEL] = label
        updateUiState(
            searchOrigin = coordinates,
            searchOriginLabel = label,
            originQuery = "",
            originPredictions = emptyList(),
        )
    }

    private fun cancelPlaceSearch() {
        searchJob?.cancel()
        searchJob = null
        selectionJob?.cancel()
        selectionJob = null
        activeSearchSession?.abandon()
        activeSearchSession = null
        selectedPlace = null
        updateUiState(isLoading = false)
    }

    private fun cancelOriginSearch() {
        originJob?.cancel()
        originJob = null
        activeOriginSession?.abandon()
        activeOriginSession = null
        updateUiState(isOriginLoading = false)
    }

    private fun updateUiState(
        predictions: List<PlacePrediction> = _uiState.value.predictions,
        selectedPlaceId: String? = _uiState.value.selectedPlaceId,
        thirdPartyAttributions: List<String> = _uiState.value.thirdPartyAttributions,
        locationState: LocationPermissionState = _uiState.value.locationState,
        mapCoordinates: GeoCoordinates = _uiState.value.mapCoordinates,
        isMapVisible: Boolean = _uiState.value.isMapVisible,
        isLoading: Boolean = _uiState.value.isLoading,
        searchOrigin: GeoCoordinates? = _uiState.value.searchOrigin,
        searchOriginLabel: String = _uiState.value.searchOriginLabel,
        originQuery: String = _uiState.value.originQuery,
        originPredictions: List<PlacePrediction> = _uiState.value.originPredictions,
        isOriginLoading: Boolean = _uiState.value.isOriginLoading,
        hasSearched: Boolean = _uiState.value.hasSearched,
        errorMessage: String? = _uiState.value.errorMessage,
    ) {
        _uiState.value = _uiState.value.copy(
            predictions = predictions,
            selectedPlaceId = selectedPlaceId,
            thirdPartyAttributions = thirdPartyAttributions,
            locationState = locationState,
            mapCoordinates = mapCoordinates,
            isMapVisible = isMapVisible,
            isLoading = isLoading,
            searchOrigin = searchOrigin,
            searchOriginLabel = searchOriginLabel,
            originQuery = originQuery,
            originPredictions = originPredictions,
            isOriginLoading = isOriginLoading,
            hasSearched = hasSearched,
            errorMessage = errorMessage,
        )
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar(message)) }
    }

    override fun onCleared() {
        activeSearchSession?.abandon()
        activeOriginSession?.abandon()
    }

    private fun PlaceSearchFailureCategory.userMessage(): String = when (this) {
        PlaceSearchFailureCategory.InvalidRequest -> "That search could not be processed."
        PlaceSearchFailureCategory.Authorization ->
            "Place search is not authorized. Check the app's Google API settings."
        PlaceSearchFailureCategory.Quota ->
            "The place-search limit has been reached. Try again later."
        PlaceSearchFailureCategory.Network ->
            "Place search needs a network connection. Check your connection and try again."
        PlaceSearchFailureCategory.NotFound -> "That place is no longer available."
        PlaceSearchFailureCategory.Unavailable,
        PlaceSearchFailureCategory.Unknown,
        -> "Place search is unavailable. Please try again."
    }

    private class SaveValidationException(message: String) : IllegalStateException(message)

    private companion object {
        const val TAG = "AddFavoriteViewModel"
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_BIAS_RADIUS_METERS = 24_140.16 // 15 miles
        const val SEARCH_QUERY = "add_search_query"
        const val ORIGIN_LATITUDE = "find_origin_latitude"
        const val ORIGIN_LONGITUDE = "find_origin_longitude"
        const val ORIGIN_LABEL = "find_origin_label"
    }

    sealed interface UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent
        data object SaveFavorite : UiEvent
    }
}
