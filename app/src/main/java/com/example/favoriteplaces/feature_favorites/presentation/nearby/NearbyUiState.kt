package com.example.favoriteplaces.feature_favorites.presentation.nearby

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyPlace
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.presentation.favorites.SavedPlaceTypeFilter

enum class NearbySource {
    OurPlaces,
    NewPlaces,
}

data class SavedNearbyFilters(
    val radiusMiles: Int = DEFAULT_SAVED_RADIUS_MILES,
    val placeType: SavedPlaceTypeFilter? = null,
    val color: Int? = null,
    val favoriteOnly: Boolean = false,
    val minimumRating: Int = 0,
) {
    val activeCount: Int
        get() = listOfNotNull(
            radiusMiles.takeIf { it != DEFAULT_SAVED_RADIUS_MILES },
            placeType,
            color,
            favoriteOnly.takeIf { it },
            minimumRating.takeIf { it > 0 },
        ).size
}

const val DEFAULT_SAVED_RADIUS_MILES = 25
const val MAX_SAVED_RADIUS_MILES = 100

data class SavedNearbyResult(
    val favorite: Favorite,
    val distanceMiles: Double?,
    val hasMapLocation: Boolean,
)

data class NearbyUiState(
    val source: NearbySource = NearbySource.OurPlaces,
    val origin: GeoCoordinates? = null,
    val originLabel: String = "Device location",
    val discoveryRadiusMiles: Int = 10,
    val discoveryCategory: NearbyCategory = NearbyCategory.All,
    val discoveryPickerCategories: List<NearbyCategory> =
        GoogleNearbyPlaceTypeCatalog.defaultPickerTypeIds.map(GoogleNearbyPlaceTypeCatalog::require),
    val discoveryPlaces: List<NearbyPlace> = emptyList(),
    val savedPlaces: Map<String, SavedNearbyPlace> = emptyMap(),
    val savedResults: List<SavedNearbyResult> = emptyList(),
    val savedFilters: SavedNearbyFilters = SavedNearbyFilters(),
    val availableSavedTypes: List<String> = emptyList(),
    val availableSavedColors: List<Int> = emptyList(),
    val hasSavedPlacesWithoutType: Boolean = false,
    val savedPlaceCount: Int = 0,
    val skippedCoordinateCount: Int = 0,
    val isMapView: Boolean = false,
    val needsLocationPermission: Boolean = true,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    val savedErrorMessage: String? = null,
    val originQuery: String = "",
    val originPredictions: List<PlacePrediction> = emptyList(),
    val isOriginLoading: Boolean = false,
) {
    val savedPlaceIds: Set<String> get() = savedPlaces.keys
    val visibleCount: Int
        get() = if (source == NearbySource.OurPlaces) savedResults.size else discoveryPlaces.size
}

data class SavedNearbyPlace(
    val favoriteId: Int,
    val isFavorite: Boolean,
)

sealed interface NearbyEvent {
    data class LocationPermissionResult(val granted: Boolean) : NearbyEvent
    data class SelectSource(val source: NearbySource) : NearbyEvent
    data class ApplyDiscoveryFilters(
        val radiusMiles: Int,
        val category: NearbyCategory,
    ) : NearbyEvent
    data class ApplySavedFilters(val filters: SavedNearbyFilters) : NearbyEvent
    data class AddDiscoveryCategory(val storageId: String) : NearbyEvent
    data class RemoveDiscoveryCategory(val storageId: String) : NearbyEvent
    data class SelectMapOrigin(val coordinates: GeoCoordinates) : NearbyEvent
    data class SavePlace(val place: NearbyPlace) : NearbyEvent
    data class ToggleDiscoveryFavorite(val placeId: String) : NearbyEvent
    data class ToggleSavedFavorite(val favoriteId: Int, val isFavorite: Boolean) : NearbyEvent
    data class EnterOriginQuery(val value: String) : NearbyEvent
    data class SelectOriginPrediction(val prediction: PlacePrediction) : NearbyEvent
    data object ToggleView : NearbyEvent
    data object Retry : NearbyEvent
    data object UseDeviceLocation : NearbyEvent
    data object SearchOrigin : NearbyEvent
}
