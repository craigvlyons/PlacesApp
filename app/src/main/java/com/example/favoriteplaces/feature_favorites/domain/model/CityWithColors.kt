package com.example.favoriteplaces.feature_favorites.domain.model

data class CityWithColors(
    val city: String,
    val colorVariations: List<ColorVariation>
)