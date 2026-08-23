package com.example.favoriteplaces.feature_favorites.presentation.citymap

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

data class CityMapUiState(
    val title: String = "Saved places",
    val places: List<Favorite> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val coordinateWarning: String? = null,
)
