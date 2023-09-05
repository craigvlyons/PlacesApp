package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.presentation.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAllCitiesUseCase(
private val repository: FavoriteRepository
) {
    suspend operator fun invoke(
        favoriteOrder: FavoriteOrder = FavoriteOrder.City(OrderType.Descending)
    ) : Flow<List<String>>? {
        return repository.getAllCities()?.map { city ->
            when (favoriteOrder.orderType) {
                is OrderType.Ascending -> {
                         city.sortedBy { it.lowercase() }
                }
                is OrderType.Descending -> {
                         city.sortedByDescending { it.lowercase() }
                }
            }
        }
    }
}