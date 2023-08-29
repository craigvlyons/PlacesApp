package com.example.favoriteplaces.feature_favorites.presentation.citymap

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

data class CityMapUiState(
    val mapItems : List<Favorite>,
)