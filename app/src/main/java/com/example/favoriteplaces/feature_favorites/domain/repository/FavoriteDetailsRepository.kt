package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoriteDetailsRepository {
    fun observeFavoriteById(id: Int): Flow<Favorite?>

    suspend fun updateNameAndType(id: Int, title: String, placeType: String?)

    suspend fun updateNotes(id: Int, notes: String?)

    suspend fun updateColor(id: Int, color: Int)

    suspend fun updateRating(id: Int, rating: Int?)

    suspend fun updateGoogleDetails(
        id: Int,
        placeId: String,
        address: String,
        city: String,
        latitude: Double,
        longitude: Double,
        phoneNumber: String?,
        googlePrimaryType: String?,
    )

    suspend fun fillMissingGoogleMetadata(
        id: Int,
        phoneNumber: String?,
        googlePrimaryType: String?,
    )
}

class SavedPlaceNotFoundException(id: Int) :
    IllegalStateException("Saved place $id no longer exists")
