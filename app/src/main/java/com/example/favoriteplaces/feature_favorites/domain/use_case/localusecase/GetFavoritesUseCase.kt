package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(private val repository: FavoriteRepository) {
    suspend operator fun invoke(): Flow<List<Favorite>> = repository.getFavorites()
}