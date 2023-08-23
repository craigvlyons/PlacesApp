package com.example.favoriteplaces.feature_favorites.data.data_source.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM Favorite")
    fun getFavorites(): Flow<List<Favorite>>

    @Query("SELECT * FROM Favorite Where id = :id")
    suspend fun getFavoriteByID(id: Int): Favorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)
}