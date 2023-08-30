package com.example.favoriteplaces.feature_favorites.presentation.citymap

import com.example.favoriteplaces.feature_favorites.domain.model.ColorVariation

data class CityMapUiState(
    val city: String,
    val mapItems : ColorVariation,
)