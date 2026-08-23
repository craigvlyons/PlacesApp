package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure

interface SavedPlaceActionMetadataRepository {
    suspend fun fetch(placeId: String): SavedPlaceActionMetadataResult
}

data class SavedPlaceActionMetadata(
    val phoneNumber: String?,
    val googlePrimaryType: String?,
)

sealed interface SavedPlaceActionMetadataResult {
    data class Success(val metadata: SavedPlaceActionMetadata) : SavedPlaceActionMetadataResult
    data class Failure(val reason: PlaceSearchFailure) : SavedPlaceActionMetadataResult
}
