package com.example.favoriteplaces.feature_favorites.data.repository

import com.example.favoriteplaces.feature_favorites.domain.model.MapItems
import com.example.favoriteplaces.feature_favorites.domain.repository.MapItemsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class MapItemsRepositoryImpl(): MapItemsRepository {
    private var mapItemsFlow: MutableStateFlow<MapItems?> = MutableStateFlow(null)

    override suspend fun getMapItems(): Flow<MapItems?> {
        return mapItemsFlow
    }

    override fun saveMapItems(mapItem: MapItems) {
        mapItemsFlow.value = mapItem
    }

   }