package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFavoritesUseCaseTest {
    private val useCase = GetFavoritesUseCase(FakeFavoriteRepository(FIXTURE))

    @Test
    fun citySortIsCaseInsensitiveInBothDirections() = runBlocking {
        assertOrder(FavoriteOrder.City(OrderType.Ascending), 2, 4, 1, 3, 5)
        assertOrder(FavoriteOrder.City(OrderType.Descending), 5, 1, 3, 4, 2)
    }

    @Test
    fun favoriteSortKeepsHeartIndependentFromColor() = runBlocking {
        assertOrder(FavoriteOrder.IsFavorite(OrderType.Ascending), 2, 4, 1, 3, 5)
        assertOrder(FavoriteOrder.IsFavorite(OrderType.Descending), 1, 3, 5, 2, 4)
    }

    @Test
    fun colorSortUsesExactStoredIntegerInBothDirections() = runBlocking {
        assertOrder(FavoriteOrder.Color(OrderType.Ascending), 4, 3, 2, 5, 1)
        assertOrder(FavoriteOrder.Color(OrderType.Descending), 1, 5, 2, 3, 4)
    }

    @Test
    fun ratingSortPreservesCurrentNullableOrdering() = runBlocking {
        assertOrder(FavoriteOrder.Rating(OrderType.Ascending), 1, 4, 2, 5, 3)
        assertOrder(FavoriteOrder.Rating(OrderType.Descending), 3, 5, 2, 4, 1)
    }

    private suspend fun assertOrder(order: FavoriteOrder, vararg expectedIds: Int) {
        val actualIds = useCase(order).first().map { it.id }
        assertEquals(expectedIds.toList(), actualIds)
    }

    private class FakeFavoriteRepository(initial: List<Favorite>) : FavoriteRepository {
        private val favorites = MutableStateFlow(initial)

        override fun getFavorites(): Flow<List<Favorite>> = favorites
        override suspend fun getFavoriteById(id: Int): Favorite? = favorites.value.firstOrNull { it.id == id }
        override suspend fun insertFavorite(favorite: Favorite) {
            favorites.value = favorites.value.filterNot { it.id == favorite.id } + favorite
        }

        override suspend fun updateIsFavorite(id: Int, isFavorite: Boolean) {
            favorites.value = favorites.value.map {
                if (it.id == id) it.copy(isFavorite = isFavorite) else it
            }
        }

        override suspend fun deleteFavorite(favorite: Favorite) {
            favorites.value = favorites.value - favorite
        }

        override fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<Favorite>> =
            MutableStateFlow(favorites.value.filter { it.city == city && it.color == color })
    }

    private companion object {
        val FIXTURE = listOf(
            Favorite(1, "p1", "One", "Address 1", null, null, true, -21615, "Denver", 1.0, 1.0),
            Favorite(2, "p2", "Two", "Address 2", null, 3, false, -1577573, "austin", 2.0, 2.0),
            Favorite(3, "p3", "Three", "Address 3", null, 5, true, -3173158, "denver", 3.0, 3.0),
            Favorite(4, "p4", "Four", "Address 4", null, 0, false, -8266006, "Boston", 4.0, 4.0),
            Favorite(5, "p5", "Five", "Address 5", null, 4, true, -749647, "Seattle", 5.0, 5.0)
        )
    }
}
