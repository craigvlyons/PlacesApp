package com.example.favoriteplaces.feature_favorites.domain.use_case.cacheusecase

import com.example.favoriteplaces.feature_favorites.domain.model.MapItems
import com.example.favoriteplaces.feature_favorites.domain.repository.MapItemsRepository

class SaveMapItemsUseCase(private val repository: MapItemsRepository) {
    operator fun invoke(mapItem: MapItems) = repository.saveMapItems(mapItem)
}