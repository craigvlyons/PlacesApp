package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.presentation.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetFavoritesUseCase(private val repository: FavoriteRepository) {
    suspend operator fun invoke(
        favoriteOrder: FavoriteOrder = FavoriteOrder.City(OrderType.Descending)
    ): Flow<List<Favorite>> {
        return repository.getFavorites().map { favorites ->
            when (favoriteOrder.orderType) {
                is OrderType.Ascending -> {
                    when (favoriteOrder) {
                        is FavoriteOrder.City -> favorites.sortedBy { it.city.lowercase() }
                        is FavoriteOrder.IsFavorite -> favorites.sortedBy { it.isFavorite }
                        is FavoriteOrder.Color -> favorites.sortedBy { it.color }
                        is FavoriteOrder.Rating -> favorites.sortedBy { it.rating }
                    }
                }

                is OrderType.Descending -> {
                    when (favoriteOrder) {
                        is FavoriteOrder.City -> favorites.sortedByDescending { it.city.lowercase() }
                        is FavoriteOrder.IsFavorite -> favorites.sortedByDescending { it.isFavorite }
                        is FavoriteOrder.Color -> favorites.sortedByDescending { it.color }
                        is FavoriteOrder.Rating -> favorites.sortedByDescending { it.rating }
                    }
                }
            }
        }
    }
}