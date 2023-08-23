package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository

class GetFavoriteUseCase(private val repository: FavoriteRepository) {
    suspend operator fun invoke(id: Int) : Favorite? = repository.getFavoriteById(id)
}