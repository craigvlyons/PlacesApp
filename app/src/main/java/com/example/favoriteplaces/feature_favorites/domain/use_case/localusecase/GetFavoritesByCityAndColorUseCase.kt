package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesByCityAndColorUseCase(
    private val repository: FavoriteRepository
) {
    suspend operator fun invoke(city: String, color : Int) : Flow<List<Favorite>>? = repository.getFavoritesByCityAndColor(city, color)

    suspend fun getColors() : Flow<List<Int>>? = repository.getAllColors()
}