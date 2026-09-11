package com.example.favoriteplaces.feature_favorites.data.data_source.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM Favorite")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM Favorite ORDER BY id")
    suspend fun getFavoritesSnapshot(): List<FavoriteEntity>

    @Query("SELECT * FROM Favorite Where id = :id")
    suspend fun getFavoriteById(id: Int): FavoriteEntity?

    @Query("SELECT * FROM Favorite WHERE id = :id")
    fun observeFavoriteById(id: Int): Flow<FavoriteEntity?>

    @Query("SELECT COUNT(*) > 0 FROM Favorite WHERE placeId = :placeId")
    suspend fun isFavoriteExists(placeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Transaction
    suspend fun insertFavoriteIfPlaceIdMissing(favorite: FavoriteEntity): Boolean {
        val placeId = favorite.placeId?.takeIf(String::isNotBlank)
        if (placeId != null && isFavoriteExists(placeId)) return false
        insertFavorite(favorite)
        return true
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFavoritesForRestore(favorites: List<FavoriteEntity>)

    @Query("UPDATE Favorite SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateIsFavorite(id: Int, isFavorite: Boolean): Int

    @Query("UPDATE Favorite SET title = :title, placeType = :placeType WHERE id = :id")
    suspend fun updateNameAndType(id: Int, title: String, placeType: String?): Int

    @Query("UPDATE Favorite SET content = :notes WHERE id = :id")
    suspend fun updateNotes(id: Int, notes: String?): Int

    @Query("UPDATE Favorite SET color = :color WHERE id = :id")
    suspend fun updateColor(id: Int, color: Int): Int

    @Query("UPDATE Favorite SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: Int, rating: Int?): Int

    @Query(
        """
        UPDATE Favorite
        SET placeId = :placeId, address = :address, city = :city,
            latitude = :latitude, longitude = :longitude, phoneNumber = :phoneNumber,
            googlePrimaryType = :googlePrimaryType
        WHERE id = :id
        """
    )
    suspend fun updateGoogleDetails(
        id: Int,
        placeId: String,
        address: String,
        city: String,
        latitude: Double,
        longitude: Double,
        phoneNumber: String?,
        googlePrimaryType: String?,
    ): Int

    @Query(
        """
        UPDATE Favorite
        SET phoneNumber = COALESCE(phoneNumber, :phoneNumber),
            googlePrimaryType = COALESCE(googlePrimaryType, :googlePrimaryType)
        WHERE id = :id AND
            ((phoneNumber IS NULL AND :phoneNumber IS NOT NULL) OR
             (googlePrimaryType IS NULL AND :googlePrimaryType IS NOT NULL))
        """
    )
    suspend fun fillMissingGoogleMetadata(
        id: Int,
        phoneNumber: String?,
        googlePrimaryType: String?,
    ): Int

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity): Int

    @Query("SELECT * FROM Favorite WHERE city = :city AND color = :color")
    fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<FavoriteEntity>>
}
