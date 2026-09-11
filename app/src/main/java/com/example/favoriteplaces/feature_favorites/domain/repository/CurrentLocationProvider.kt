package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates

fun interface CurrentLocationProvider {
    suspend fun currentLocation(): GeoCoordinates?
}
