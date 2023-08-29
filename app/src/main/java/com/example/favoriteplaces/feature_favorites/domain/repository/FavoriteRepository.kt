package com.example.favoriteplaces.feature_favorites.domain.repository

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavorites(): Flow<List<Favorite>>

    suspend fun getFavoriteById(id: Int): Favorite?

    suspend fun insertFavorite(favorite: Favorite)

    suspend fun updateIsFavorite(id: Int, isFavorite: Boolean)

    suspend fun deleteFavorite(favorite: Favorite)

    fun getAllCities(): Flow<List<String>>?
    fun getAllColors(): Flow<List<Int>>?

    fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<Favorite>>?
}