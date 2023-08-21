package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import com.example.favoriteplaces.feature_favorites.data.models.Prediction
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.google.android.gms.maps.model.LatLng


data class AddNewFavoriteUiState(
    val predictions: List<Prediction> = emptyList(),
    val favorite: Favorite? = null,
    val currentLocation: LatLng = LatLng(0.0, 0.0)
    // Add more UI-related properties as needed
)