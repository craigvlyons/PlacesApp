package com.example.favoriteplaces.feature_favorites.data.repository

import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDao
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class FavoriteRepositoryImpl(
    private val dao: FavoriteDao
): FavoriteRepository {
    override fun getFavorites(): Flow<List<Favorite>> {
        return dao.getFavorites()
    }

    override suspend fun getFavoriteById(id: Int): Favorite? {
        return dao.getFavoriteByID(id)
    }

    override suspend fun insertFavorite(favorite: Favorite) {
        dao.insertFavorite(favorite)
    }

    override suspend fun deleteFavorite(favorite: Favorite) {
        dao.deleteFavorite(favorite)
    }

}