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

    @Query("UPDATE Favorite SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateIsFavorite( id: Int, isFavorite: Boolean )

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)

    @Query("SELECT DISTINCT city FROM Favorite")
    fun getAllCities(): Flow<List<String>>?

    @Query("SELECT DISTINCT color FROM Favorite")
    fun getAllColors(): Flow<List<Int>>?

    @Query("SELECT * FROM Favorite WHERE city = :city AND color = :color")
    fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<Favorite>>?
}