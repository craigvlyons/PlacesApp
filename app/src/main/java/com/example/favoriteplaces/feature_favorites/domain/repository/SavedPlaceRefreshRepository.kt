package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure

interface SavedPlaceRefreshRepository {
    suspend fun refresh(placeId: String): SavedPlaceRefreshResult
}

data class GooglePlaceDetails(
    val placeId: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val phoneNumber: String?,
    val googlePrimaryType: String? = null,
)

sealed interface SavedPlaceRefreshResult {
    data class Success(val details: GooglePlaceDetails) : SavedPlaceRefreshResult
    data class Failure(val reason: PlaceSearchFailure) : SavedPlaceRefreshResult
}
