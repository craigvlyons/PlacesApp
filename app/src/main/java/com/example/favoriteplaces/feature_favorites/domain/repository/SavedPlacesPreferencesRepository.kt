package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import kotlinx.coroutines.flow.Flow

interface SavedPlacesPreferencesRepository {
    val preferences: Flow<SavedPlacesPreferences>

    suspend fun setListView(isListView: Boolean)

    suspend fun setFavoriteOrder(favoriteOrder: FavoriteOrder)

    suspend fun setAppColor(color: Int)

    suspend fun setDefaultCardColor(color: Int)

    suspend fun setNearbyDiscoveryTypeIds(typeIds: List<String>)
}
