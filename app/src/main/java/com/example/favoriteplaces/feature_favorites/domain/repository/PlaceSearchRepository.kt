package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchOrigin
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.SelectedPlace

interface PlaceSearchRepository {
    fun createSession(): PlaceSearchSession
}

interface PlaceSearchSession {
    suspend fun search(
        query: String,
        origin: PlaceSearchOrigin? = null,
    ): PlaceSearchResult<List<PlacePrediction>>

    suspend fun select(placeId: String): PlaceSearchResult<SelectedPlace>

    fun abandon()
}
