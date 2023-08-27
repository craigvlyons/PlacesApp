package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository

class UpdateIsFavorite(private  val repository: FavoriteRepository) {
    suspend operator fun invoke(id: Int, isFavorite: Boolean) = repository.updateIsFavorite(id, isFavorite)
}