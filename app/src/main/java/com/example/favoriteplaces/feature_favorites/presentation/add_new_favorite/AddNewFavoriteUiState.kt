package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates


data class AddNewFavoriteUiState(
    val searchField: AddFavoriteTextFieldState = AddFavoriteTextFieldState(
        hint = "Search for place..."
    ),
    val predictions: List<PlacePrediction> = emptyList(),
    val selectedPlaceId: String? = null,
    val thirdPartyAttributions: List<String> = emptyList(),
    val locationState: LocationPermissionState = LocationPermissionState.NoPermission,
    val mapCoordinates: GeoCoordinates = GeoCoordinates(0.0, 0.0),
    val isMapVisible: Boolean = false,
    val isLoading: Boolean = false,
    val searchOrigin: GeoCoordinates? = null,
    val searchOriginLabel: String = "Location unavailable",
    val originQuery: String = "",
    val originPredictions: List<PlacePrediction> = emptyList(),
    val isOriginLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
)
