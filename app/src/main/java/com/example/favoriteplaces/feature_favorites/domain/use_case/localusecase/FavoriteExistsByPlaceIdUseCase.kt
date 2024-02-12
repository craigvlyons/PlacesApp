package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository

class FavoriteExistsByPlaceIdUseCase(private val repository: FavoriteRepository) {
    suspend operator fun invoke(placeId: String) : Boolean = repository.checkFavoriteExistsByPlaceId(placeId)
}