package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyPlace
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbySearchRequest
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult

interface NearbyPlacesRepository {
    suspend fun search(request: NearbySearchRequest): PlaceSearchResult<List<NearbyPlace>>
}
