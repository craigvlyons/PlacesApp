package com.example.favoriteplaces.feature_favorites.data.repository

import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDao
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toDomain
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toEntity
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.settings.isValidArgbColor
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteDetailsRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceAlreadyExistsException
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class FavoriteRepositoryImpl(
    private val dao: FavoriteDao
): FavoriteRepository, FavoriteDetailsRepository {
    override fun getFavorites(): Flow<List<Favorite>> {
        return dao.getFavorites().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getFavoriteById(id: Int): Favorite? {
        return dao.getFavoriteById(id)?.toDomain()
    }

    override suspend fun insertFavorite(favorite: Favorite) {
        if (!dao.insertFavoriteIfPlaceIdMissing(favorite.toEntity())) {
            throw SavedPlaceAlreadyExistsException()
        }
    }

    override suspend fun updateIsFavorite(id: Int, isFavorite: Boolean) {
        dao.updateIsFavorite(id, isFavorite).requireUpdated(id)
    }

    override fun observeFavoriteById(id: Int): Flow<Favorite?> =
        dao.observeFavoriteById(id).map { it?.toDomain() }

    override suspend fun updateNameAndType(id: Int, title: String, placeType: String?) {
        require(title.isNotBlank()) { "A saved place name cannot be blank." }
        dao.updateNameAndType(id, title, placeType).requireUpdated(id)
    }

    override suspend fun updateNotes(id: Int, notes: String?) {
        dao.updateNotes(id, notes).requireUpdated(id)
    }

    override suspend fun updateColor(id: Int, color: Int) {
        require(color.isValidArgbColor()) { "A saved place color must be opaque." }
        dao.updateColor(id, color).requireUpdated(id)
    }

    override suspend fun updateRating(id: Int, rating: Int?) {
        require(rating == null || rating in 1..5) { "A rating must be null or between 1 and 5." }
        dao.updateRating(id, rating).requireUpdated(id)
    }

    override suspend fun updateGoogleDetails(
        id: Int,
        placeId: String,
        address: String,
        city: String,
        latitude: Double,
        longitude: Double,
        phoneNumber: String?,
        googlePrimaryType: String?,
    ) {
        require(placeId.isNotBlank()) { "A Google place ID cannot be blank." }
        require(address.isNotBlank()) { "A Google address cannot be blank." }
        require(city.isNotBlank()) { "A Google address must include a city." }
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0) {
            "Google coordinates are outside the valid range."
        }
        dao.updateGoogleDetails(
            id = id,
            placeId = placeId.trim(),
            address = address.trim(),
            city = city.trim(),
            latitude = latitude,
            longitude = longitude,
            phoneNumber = phoneNumber?.trim()?.takeIf(String::isNotEmpty),
            googlePrimaryType = googlePrimaryType?.trim()?.lowercase(Locale.US)?.takeIf(String::isNotEmpty),
        ).requireUpdated(id)
    }

    override suspend fun fillMissingGoogleMetadata(
        id: Int,
        phoneNumber: String?,
        googlePrimaryType: String?,
    ) {
        dao.fillMissingGoogleMetadata(
            id = id,
            phoneNumber = phoneNumber?.trim()?.takeIf(String::isNotEmpty),
            googlePrimaryType = googlePrimaryType
                ?.trim()
                ?.lowercase(Locale.US)
                ?.takeIf(String::isNotEmpty),
        )
    }

    override suspend fun deleteFavorite(favorite: Favorite) {
        dao.deleteFavorite(favorite.toEntity()).requireUpdated(favorite.id ?: -1)
    }

    override fun getFavoritesByCityAndColor(
        city: String,
        color: Int
    ): Flow<List<Favorite>> {
        return dao.getFavoritesByCityAndColor(city, color).map { entities ->
            entities.map { it.toDomain() }
        }
    }

}

private fun Int.requireUpdated(id: Int) {
    if (this != 1) throw SavedPlaceNotFoundException(id)
}
