package com.example.favoriteplaces.testutil

import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSavedPlacesPreferencesRepository(
    initial: SavedPlacesPreferences = SavedPlacesPreferences(),
) : SavedPlacesPreferencesRepository {
    val value = MutableStateFlow(initial)
    override val preferences: Flow<SavedPlacesPreferences> = value

    override suspend fun setListView(isListView: Boolean) {
        value.value = value.value.copy(isListView = isListView)
    }

    override suspend fun setFavoriteOrder(favoriteOrder: FavoriteOrder) {
        value.value = value.value.copy(favoriteOrder = favoriteOrder)
    }

    override suspend fun setAppColor(color: Int) {
        value.value = value.value.copy(appColor = color)
    }

    override suspend fun setDefaultCardColor(color: Int) {
        value.value = value.value.copy(defaultCardColor = color)
    }

    override suspend fun setNearbyDiscoveryTypeIds(typeIds: List<String>) {
        value.value = value.value.copy(nearbyDiscoveryTypeIds = typeIds)
    }
}
