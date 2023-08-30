package com.example.favoriteplaces.feature_favorites.domain.use_case.cacheusecase

data class MapItemsCacheUseCases(
    val getMapItems: GetMapItemsUseCase,
    val saveMapItems: SaveMapItemsUseCase
)