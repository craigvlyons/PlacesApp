package com.example.favoriteplaces.feature_favorites.domain.use_case.cacheusecase

import com.example.favoriteplaces.feature_favorites.domain.model.MapItems
import com.example.favoriteplaces.feature_favorites.domain.repository.MapItemsRepository
import kotlinx.coroutines.flow.Flow

class GetMapItemsUseCase(private val repository: MapItemsRepository) {
    suspend operator fun invoke(): Flow<MapItems?> = repository.getMapItems()
}