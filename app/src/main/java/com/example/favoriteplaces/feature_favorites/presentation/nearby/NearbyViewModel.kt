package com.example.favoriteplaces.feature_favorites.presentation.nearby

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.distanceMilesTo
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyPlace
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbySearchRequest
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceTypeFormatter
import com.example.favoriteplaces.feature_favorites.domain.repository.CurrentLocationProvider
import com.example.favoriteplaces.feature_favorites.domain.repository.NearbyPlacesRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchSession
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceAlreadyExistsException
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshResult
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.domain.use_case.settings.NearbyTypePickerUpdateResult
import com.example.favoriteplaces.feature_favorites.domain.use_case.settings.UpdateNearbyTypePickerUseCase
import com.example.favoriteplaces.feature_favorites.presentation.favorites.SavedPlaceTypeFilter
import com.example.favoriteplaces.feature_favorites.presentation.util.PlaceCityResolver
import com.example.favoriteplaces.feature_favorites.presentation.util.restoreGeoCoordinates
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val locationProvider: CurrentLocationProvider,
    private val nearbyRepository: NearbyPlacesRepository,
    private val favoriteUseCases: FavoriteUseCases,
    private val preferencesRepository: SavedPlacesPreferencesRepository,
    private val placeSearchRepository: PlaceSearchRepository,
    private val updateNearbyTypePicker: UpdateNearbyTypePickerUseCase,
    private val savedPlaceRefreshRepository: SavedPlaceRefreshRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var discoveryJob: Job? = null
    private var locationJob: Job? = null
    private val saveJobs = mutableMapOf<String, Job>()
    private var savedPlacesJob: Job? = null
    private var deviceLocation: GeoCoordinates? = null
    private var originSession: PlaceSearchSession? = null
    private var originJob: Job? = null
    private val favoriteJobs = mutableMapOf<Int, Job>()
    private var allFavorites: List<Favorite> = emptyList()
    private var lastSuccessfulDiscovery: CachedDiscovery? = null
    private var activeDiscoveryRequest: DiscoveryRequestKey? = null
    private var hasLoadedPickerPreferences = false

    private val restoredOrigin = savedStateHandle.restoreGeoCoordinates(
        ORIGIN_LATITUDE,
        ORIGIN_LONGITUDE,
        TAG,
    )
    private val restoredCategory = NearbyCategory.fromStorageValue(savedStateHandle[CATEGORY])
    private val restoredSource = savedStateHandle.get<String>(SOURCE)
        ?.let { stored -> NearbySource.entries.firstOrNull { it.name == stored } }
        ?: NearbySource.OurPlaces
    private val restoredSavedFilters = SavedNearbyFilters(
        radiusMiles = savedStateHandle.get<Int>(SAVED_RADIUS)
            ?.coerceIn(1, MAX_SAVED_RADIUS_MILES)
            ?: DEFAULT_SAVED_RADIUS_MILES,
        placeType = savedStateHandle.get<String>(SAVED_TYPE)?.let { stored ->
            if (stored == TYPE_NOT_SET) SavedPlaceTypeFilter.NotSet
            else SavedPlaceTypeFilter.Exact(stored)
        },
        color = savedStateHandle.get<Int>(SAVED_COLOR),
        favoriteOnly = savedStateHandle.get<Boolean>(SAVED_FAVORITE) ?: false,
        minimumRating = savedStateHandle.get<Int>(SAVED_RATING)?.coerceIn(0, 5) ?: 0,
    )

    private val _state = MutableStateFlow(
        NearbyUiState(
            source = restoredSource,
            origin = restoredOrigin,
            originLabel = savedStateHandle.get<String>(ORIGIN_LABEL) ?: "Device location",
            discoveryRadiusMiles = savedStateHandle.get<Int>(RADIUS)?.coerceIn(1, 31) ?: 10,
            discoveryCategory = restoredCategory,
            savedFilters = restoredSavedFilters,
            isMapView = savedStateHandle.get<Boolean>(MAP_VIEW) ?: false,
            needsLocationPermission = restoredOrigin == null,
        )
    )
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        observeNearbyTypePreferences()
        observeSavedPlaces()
    }

    fun onEvent(event: NearbyEvent) {
        when (event) {
            is NearbyEvent.LocationPermissionResult -> {
                _state.update { it.copy(needsLocationPermission = false) }
                if (event.granted) loadDeviceLocation()
            }
            is NearbyEvent.SelectSource -> selectSource(event.source)
            is NearbyEvent.ApplyDiscoveryFilters -> {
                val radius = event.radiusMiles.coerceIn(1, 31)
                val category = event.category.takeIf { requested ->
                    requested == NearbyCategory.All ||
                        _state.value.discoveryPickerCategories.any { it.storageId == requested.storageId }
                } ?: NearbyCategory.All
                savedStateHandle[RADIUS] = radius
                savedStateHandle[CATEGORY] = category.storageId
                _state.update {
                    it.copy(
                        discoveryRadiusMiles = radius,
                        discoveryCategory = category,
                        errorMessage = null,
                    )
                }
                searchDiscoveryIfNeeded()
            }
            is NearbyEvent.AddDiscoveryCategory -> updateDiscoveryPicker(event.storageId, add = true)
            is NearbyEvent.RemoveDiscoveryCategory -> updateDiscoveryPicker(event.storageId, add = false)
            is NearbyEvent.ApplySavedFilters -> {
                val filters = event.filters.normalized()
                savedStateHandle[SAVED_RADIUS] = filters.radiusMiles
                savedStateHandle[SAVED_TYPE] = when (val type = filters.placeType) {
                    null -> null
                    SavedPlaceTypeFilter.NotSet -> TYPE_NOT_SET
                    is SavedPlaceTypeFilter.Exact -> type.value
                }
                savedStateHandle[SAVED_COLOR] = filters.color
                savedStateHandle[SAVED_FAVORITE] = filters.favoriteOnly
                savedStateHandle[SAVED_RATING] = filters.minimumRating
                _state.update { it.copy(savedFilters = filters) }
                updateSavedProjection()
            }
            is NearbyEvent.SelectMapOrigin -> {
                cancelOriginSearch()
                setOrigin(event.coordinates, "Selected map location")
            }
            is NearbyEvent.SavePlace -> savePlace(event.place)
            is NearbyEvent.ToggleDiscoveryFavorite -> toggleDiscoveryFavorite(event.placeId)
            is NearbyEvent.ToggleSavedFavorite -> toggleSavedFavorite(event.favoriteId, event.isFavorite)
            is NearbyEvent.EnterOriginQuery -> {
                cancelOriginSearch()
                _state.update {
                    it.copy(
                        originQuery = event.value,
                        originPredictions = emptyList(),
                        isOriginLoading = false,
                    )
                }
            }
            is NearbyEvent.SelectOriginPrediction -> selectOriginPrediction(event.prediction)
            NearbyEvent.ToggleView -> {
                val mapView = !_state.value.isMapView
                savedStateHandle[MAP_VIEW] = mapView
                _state.update { it.copy(isMapView = mapView) }
            }
            NearbyEvent.Retry -> {
                if (_state.value.source == NearbySource.NewPlaces) {
                    searchDiscoveryIfNeeded(force = true)
                } else {
                    if (_state.value.savedErrorMessage != null) observeSavedPlaces(force = true)
                    else updateSavedProjection()
                }
            }
            NearbyEvent.UseDeviceLocation -> {
                cancelOriginSearch()
                deviceLocation?.let { setOrigin(it, "Device location") } ?: loadDeviceLocation()
            }
            NearbyEvent.SearchOrigin -> searchOrigins()
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch { _events.emit(UiEvent.Message(message)) }
    }

    private fun observeNearbyTypePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    PrivacySafeLog.error(TAG, "Failed to observe Nearby preferences", exception)
                    hasLoadedPickerPreferences = true
                    _events.emit(UiEvent.Message("Nearby settings could not be loaded."))
                    searchDiscoveryIfNeeded()
                }
                .collect { preferences ->
                val isFirstLoad = !hasLoadedPickerPreferences
                val pickerCategories = GoogleNearbyPlaceTypeCatalog
                    .normalizePickerTypeIds(preferences.nearbyDiscoveryTypeIds)
                    .mapNotNull(GoogleNearbyPlaceTypeCatalog::find)
                val current = _state.value
                val selectedStillAvailable = current.discoveryCategory == NearbyCategory.All ||
                    pickerCategories.any { it.storageId == current.discoveryCategory.storageId }
                val nextCategory = current.discoveryCategory.takeIf { selectedStillAvailable }
                    ?: NearbyCategory.All
                if (nextCategory != current.discoveryCategory) {
                    savedStateHandle[CATEGORY] = nextCategory.storageId
                }
                _state.update {
                    it.copy(
                        discoveryPickerCategories = pickerCategories,
                        discoveryCategory = nextCategory,
                    )
                }
                hasLoadedPickerPreferences = true
                if (isFirstLoad || nextCategory != current.discoveryCategory) {
                    searchDiscoveryIfNeeded()
                }
            }
        }
    }

    private fun updateDiscoveryPicker(storageId: String, add: Boolean) {
        val category = GoogleNearbyPlaceTypeCatalog.find(storageId)
        if (category == null) {
            viewModelScope.launch { _events.emit(UiEvent.Message("That place type is not supported.")) }
            return
        }
        viewModelScope.launch {
            try {
                when (updateNearbyTypePicker(category.storageId, add)) {
                    NearbyTypePickerUpdateResult.LimitReached -> _events.emit(
                        UiEvent.Message(
                            "Remove a type before adding another. The quick picker holds " +
                                "${GoogleNearbyPlaceTypeCatalog.MAX_PICKER_TYPES}."
                        )
                    )
                    NearbyTypePickerUpdateResult.Unsupported ->
                        _events.emit(UiEvent.Message("That place type is not supported."))
                    NearbyTypePickerUpdateResult.Updated,
                    NearbyTypePickerUpdateResult.Unchanged -> Unit
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to update Nearby type picker preferences", exception)
                _events.emit(UiEvent.Message("Place-type choices could not be updated."))
            }
        }
    }

    private fun selectSource(source: NearbySource) {
        if (_state.value.source == source) return
        savedStateHandle[SOURCE] = source.name
        _state.update { it.copy(source = source, errorMessage = null) }
        if (source == NearbySource.NewPlaces) {
            searchDiscoveryIfNeeded()
        } else {
            activeDiscoveryRequest = null
            discoveryJob?.cancel()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun searchOrigins() {
        val query = _state.value.originQuery.trim()
        if (query.length < 2) {
            viewModelScope.launch { _events.emit(UiEvent.Message("Enter at least 2 characters.")) }
            return
        }
        cancelOriginSearch()
        val session = placeSearchRepository.createSession()
        originSession = session
        originJob = viewModelScope.launch {
            _state.update { it.copy(isOriginLoading = true, originPredictions = emptyList()) }
            try {
                when (val result = session.search(query)) {
                    is PlaceSearchResult.Success -> _state.update {
                        it.copy(originPredictions = result.value, isOriginLoading = false)
                    }
                    is PlaceSearchResult.Failure -> {
                        _state.update { it.copy(isOriginLoading = false) }
                        _events.emit(UiEvent.Message(result.reason.category.userMessage()))
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Location search failed", exception)
                _events.emit(UiEvent.Message("Locations could not be searched."))
            } finally {
                if (originSession === session) {
                    _state.update { it.copy(isOriginLoading = false) }
                }
            }
        }
    }

    private fun selectOriginPrediction(prediction: PlacePrediction) {
        val session = originSession ?: return
        originJob?.cancel()
        originJob = viewModelScope.launch {
            _state.update { it.copy(isOriginLoading = true) }
            try {
                when (val result = session.select(prediction.placeId)) {
                    is PlaceSearchResult.Success -> setOrigin(
                        GeoCoordinates(result.value.latitude, result.value.longitude),
                        result.value.displayName,
                    )
                    is PlaceSearchResult.Failure ->
                        _events.emit(UiEvent.Message(result.reason.category.userMessage()))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Location selection failed", exception)
                _events.emit(UiEvent.Message("That location could not be selected."))
            } finally {
                if (originSession === session) {
                    originSession = null
                    _state.update { it.copy(isOriginLoading = false) }
                }
            }
        }
    }

    private fun cancelOriginSearch() {
        originJob?.cancel()
        originJob = null
        originSession?.abandon()
        originSession = null
        _state.update { it.copy(isOriginLoading = false) }
    }

    private fun observeSavedPlaces(force: Boolean = false) {
        if (!force && savedPlacesJob?.isActive == true) return
        savedPlacesJob?.cancel()
        savedPlacesJob = viewModelScope.launch {
            favoriteUseCases.getFavorites.observe()
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    PrivacySafeLog.error(TAG, "Failed to observe saved places for Nearby", exception)
                    _state.update { it.copy(savedErrorMessage = "Saved places could not be loaded.") }
                    _events.emit(UiEvent.Message("Saved places could not be loaded."))
                }
                .collect { favorites ->
                    allFavorites = favorites
                    _state.update {
                        it.copy(
                            savedPlaces = favorites.mapNotNull { favorite ->
                                val placeId = favorite.placeId ?: return@mapNotNull null
                                val id = favorite.id ?: return@mapNotNull null
                                placeId to SavedNearbyPlace(id, favorite.isFavorite)
                            }.toMap(),
                            savedErrorMessage = null,
                        )
                    }
                    updateSavedProjection()
                }
        }
    }

    private fun updateSavedProjection() {
        val current = _state.value
        val valid = allFavorites.filter { it.hasUsableCoordinates() }
        val projected = allFavorites.map { favorite ->
            val hasMapLocation = favorite.hasUsableCoordinates()
            SavedNearbyResult(
                favorite = favorite,
                distanceMiles = current.origin?.takeIf { hasMapLocation }?.let { origin ->
                    origin.distanceMilesTo(GeoCoordinates(favorite.latitude, favorite.longitude))
                },
                hasMapLocation = hasMapLocation,
            )
        }.filter { result -> result.matches(current.savedFilters) }
            .sortedWith(compareBy<SavedNearbyResult> { it.distanceMiles ?: Double.MAX_VALUE }
                .thenBy { it.favorite.title.lowercase() }
                .thenBy { it.favorite.id ?: Int.MAX_VALUE })

        _state.update {
            it.copy(
                savedResults = projected,
                savedPlaceCount = allFavorites.size,
                skippedCoordinateCount = allFavorites.size - valid.size,
                availableSavedTypes = allFavorites.mapNotNull(Favorite::placeType)
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .distinctBy(String::lowercase)
                    .sortedWith(String.CASE_INSENSITIVE_ORDER),
                availableSavedColors = allFavorites.map(Favorite::color).distinct(),
                hasSavedPlacesWithoutType = allFavorites.any { favorite -> favorite.placeType.isNullOrBlank() },
            )
        }
    }

    private fun loadDeviceLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.source == NearbySource.NewPlaces, errorMessage = null) }
            try {
                val location = locationProvider.currentLocation()
                if (location == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (it.source == NearbySource.NewPlaces) {
                                "Current location is unavailable. Choose another location."
                            } else null,
                        )
                    }
                } else {
                    deviceLocation = location
                    setOrigin(location, "Device location")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SecurityException) {
                _state.update { it.copy(isLoading = false, needsLocationPermission = true) }
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Nearby device location failed", exception)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (it.source == NearbySource.NewPlaces) {
                            "Current location is unavailable. Choose another location."
                        } else null,
                    )
                }
            }
        }
    }

    private fun setOrigin(coordinates: GeoCoordinates, label: String) {
        savedStateHandle[ORIGIN_LATITUDE] = coordinates.latitude
        savedStateHandle[ORIGIN_LONGITUDE] = coordinates.longitude
        savedStateHandle[ORIGIN_LABEL] = label
        _state.update {
            it.copy(
                origin = coordinates,
                originLabel = label,
                originQuery = "",
                originPredictions = emptyList(),
                errorMessage = null,
            )
        }
        updateSavedProjection()
        searchDiscoveryIfNeeded()
    }

    private fun searchDiscoveryIfNeeded(force: Boolean = false) {
        if (!hasLoadedPickerPreferences) return
        val current = _state.value
        if (current.source != NearbySource.NewPlaces) return
        val origin = current.origin
        if (origin == null) {
            activeDiscoveryRequest = null
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Choose a location to find new places nearby.",
                )
            }
            return
        }
        val key = DiscoveryRequestKey(origin, current.discoveryRadiusMiles, current.discoveryCategory)
        val cached = lastSuccessfulDiscovery
        if (!force && cached?.key == key) {
            activeDiscoveryRequest = null
            discoveryJob?.cancel()
            _state.update {
                it.copy(
                    discoveryPlaces = cached.places,
                    isLoading = false,
                    hasSearched = true,
                    errorMessage = null,
                )
            }
            return
        }

        activeDiscoveryRequest = key
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    discoveryPlaces = emptyList(),
                    isLoading = true,
                    errorMessage = null,
                )
            }
            try {
                when (
                    val result = nearbyRepository.search(
                        NearbySearchRequest(origin, current.discoveryRadiusMiles, current.discoveryCategory)
                    )
                ) {
                    is PlaceSearchResult.Success -> {
                        if (activeDiscoveryRequest != key || _state.value.source != NearbySource.NewPlaces) {
                            return@launch
                        }
                        val places = result.value.sortedBy(NearbyPlace::distanceMiles)
                        lastSuccessfulDiscovery = CachedDiscovery(key, places)
                        activeDiscoveryRequest = null
                        _state.update {
                            it.copy(
                                discoveryPlaces = places,
                                isLoading = false,
                                hasSearched = true,
                            )
                        }
                    }
                    is PlaceSearchResult.Failure -> {
                        if (activeDiscoveryRequest != key || _state.value.source != NearbySource.NewPlaces) {
                            return@launch
                        }
                        activeDiscoveryRequest = null
                        _state.update {
                            it.copy(
                                discoveryPlaces = emptyList(),
                                isLoading = false,
                                hasSearched = true,
                                errorMessage = result.reason.category.userMessage(),
                            )
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Unexpected Nearby discovery failure", exception)
                if (activeDiscoveryRequest == key && _state.value.source == NearbySource.NewPlaces) {
                    activeDiscoveryRequest = null
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasSearched = true,
                            errorMessage = "Nearby places are unavailable. Please try again.",
                        )
                    }
                }
            }
        }
    }

    private fun savePlace(place: NearbyPlace) {
        if (saveJobs[place.placeId]?.isActive == true || place.placeId in _state.value.savedPlaceIds) return
        saveJobs[place.placeId] = viewModelScope.launch {
            try {
                val refreshedDetails = when (
                    val result = savedPlaceRefreshRepository.refresh(place.placeId)
                ) {
                    is SavedPlaceRefreshResult.Success -> result.details
                    is SavedPlaceRefreshResult.Failure -> null
                }
                val city = PlaceCityResolver.resolve(place.addressComponents)
                if (city == null) {
                    _events.emit(UiEvent.Message("Google did not provide a city for this place."))
                    return@launch
                }
                favoriteUseCases.addFavorite(
                    Favorite(
                        placeId = place.placeId,
                        title = place.displayName,
                        address = place.formattedAddress,
                        content = "",
                        rating = 0,
                        city = city,
                        latitude = place.coordinates.latitude,
                        longitude = place.coordinates.longitude,
                        placeType = PlaceTypeFormatter.fromGoogleTypes(listOf(place.primaryType)),
                        color = preferencesRepository.preferences.first().defaultCardColor,
                        phoneNumber = refreshedDetails?.phoneNumber,
                        googlePrimaryType = refreshedDetails?.googlePrimaryType
                            ?: place.primaryType,
                    )
                )
                _events.emit(
                    UiEvent.Message(
                        if (refreshedDetails == null) {
                            "Place saved. Phone details can be added later from Edit."
                        } else {
                            "Place saved."
                        }
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: SavedPlaceAlreadyExistsException) {
                _events.emit(UiEvent.Message("That place is already saved."))
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to save nearby place", exception)
                _events.emit(UiEvent.Message("The place could not be saved."))
            } finally {
                saveJobs.remove(place.placeId)
            }
        }
    }

    private fun toggleDiscoveryFavorite(placeId: String) {
        val saved = _state.value.savedPlaces[placeId] ?: return
        toggleSavedFavorite(saved.favoriteId, saved.isFavorite)
    }

    private fun toggleSavedFavorite(favoriteId: Int, isFavorite: Boolean) {
        if (favoriteJobs[favoriteId]?.isActive == true) return
        favoriteJobs[favoriteId] = viewModelScope.launch {
            try {
                favoriteUseCases.updateIsFavorite(favoriteId, !isFavorite)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to update Nearby Favorite heart", exception)
                _events.emit(UiEvent.Message("The Favorite heart could not be updated."))
            } finally {
                favoriteJobs.remove(favoriteId)
            }
        }
    }

    private fun Favorite.hasUsableCoordinates(): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    private fun SavedNearbyResult.matches(filters: SavedNearbyFilters): Boolean {
        val typeMatches = when (val type = filters.placeType) {
            null -> true
            SavedPlaceTypeFilter.NotSet -> favorite.placeType.isNullOrBlank()
            is SavedPlaceTypeFilter.Exact ->
                favorite.placeType?.trim()?.equals(type.value, ignoreCase = true) == true
        }
        return typeMatches &&
            (filters.color == null || favorite.color == filters.color) &&
            (!filters.favoriteOnly || favorite.isFavorite) &&
            ((favorite.rating ?: 0) >= filters.minimumRating) &&
            (distanceMiles?.let { it <= filters.radiusMiles } == true)
    }

    private fun SavedNearbyFilters.normalized() = copy(
        radiusMiles = radiusMiles.coerceIn(1, MAX_SAVED_RADIUS_MILES),
        minimumRating = minimumRating.coerceIn(0, 5),
    )

    private fun PlaceSearchFailureCategory.userMessage(): String = when (this) {
        PlaceSearchFailureCategory.Network -> "Nearby search needs a network connection."
        PlaceSearchFailureCategory.Authorization -> "Nearby search is not authorized."
        PlaceSearchFailureCategory.Quota -> "The nearby-search limit has been reached."
        PlaceSearchFailureCategory.InvalidRequest -> "Those nearby filters could not be used."
        else -> "Nearby places are unavailable. Please try again."
    }

    sealed interface UiEvent { data class Message(val value: String) : UiEvent }

    private data class DiscoveryRequestKey(
        val origin: GeoCoordinates,
        val radiusMiles: Int,
        val category: NearbyCategory,
    )

    private data class CachedDiscovery(
        val key: DiscoveryRequestKey,
        val places: List<NearbyPlace>,
    )

    override fun onCleared() {
        originSession?.abandon()
    }

    private companion object {
        const val TAG = "NearbyViewModel"
        const val ORIGIN_LATITUDE = "nearby_origin_latitude"
        const val ORIGIN_LONGITUDE = "nearby_origin_longitude"
        const val ORIGIN_LABEL = "nearby_origin_label"
        const val RADIUS = "nearby_radius"
        const val CATEGORY = "nearby_category"
        const val MAP_VIEW = "nearby_map_view"
        const val SOURCE = "nearby_source"
        const val SAVED_RADIUS = "nearby_saved_radius"
        const val SAVED_TYPE = "nearby_saved_type"
        const val SAVED_COLOR = "nearby_saved_color"
        const val SAVED_FAVORITE = "nearby_saved_favorite"
        const val SAVED_RATING = "nearby_saved_rating"
        const val TYPE_NOT_SET = "__TYPE_NOT_SET__"
    }
}
