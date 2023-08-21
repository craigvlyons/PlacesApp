package com.example.favoriteplaces.feature_favorites.domain.repository

interface PlacesRepository {
    suspend fun getPredictions()
}