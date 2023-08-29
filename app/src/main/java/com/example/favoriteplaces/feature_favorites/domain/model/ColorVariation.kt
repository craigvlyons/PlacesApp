package com.example.favoriteplaces.feature_favorites.domain.model

data class ColorVariation(
    val color: Int,
    val favorites: List<Favorite>
)