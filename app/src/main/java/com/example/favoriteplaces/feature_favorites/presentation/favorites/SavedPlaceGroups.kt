package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

data class CityGroupUiModel(
    val city: String,
    val colorVariations: List<ColorGroupUiModel>,
)

data class ColorGroupUiModel(
    val color: Int,
    val favorites: List<Favorite>,
)
