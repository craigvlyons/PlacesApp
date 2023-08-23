package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository

class DeleteFavoriteUseCase(private val repository: FavoriteRepository) {
    suspend operator fun invoke(favorite: Favorite) = repository.deleteFavorite(favorite)
}