package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.MapItems
import kotlinx.coroutines.flow.Flow

interface MapItemsRepository {
    suspend fun getMapItems(): Flow<MapItems?>

    fun saveMapItems(mapItems: MapItems)
}