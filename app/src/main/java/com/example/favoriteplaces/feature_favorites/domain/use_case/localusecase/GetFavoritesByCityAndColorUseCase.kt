package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesByCityAndColorUseCase(
    private val repository: FavoriteRepository
) {
    suspend operator fun invoke() : Flow<List<String>>? = repository.getAllCities()
}