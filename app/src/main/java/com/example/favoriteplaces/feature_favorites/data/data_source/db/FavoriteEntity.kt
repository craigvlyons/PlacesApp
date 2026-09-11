package com.example.favoriteplaces.feature_favorites.data.data_source.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

@Entity(tableName = "Favorite")
data class FavoriteEntity(
    @PrimaryKey
    val id: Int? = null,
    val placeId: String? = null,
    val title: String,
    val address: String,
    val content: String?,
    val rating: Int?,
    val isFavorite: Boolean = false,
    val color: Int = Favorite.DEFAULT_COLOR_ARGB,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val placeType: String? = null,
    val phoneNumber: String? = null,
    val googlePrimaryType: String? = null,
)

fun FavoriteEntity.toDomain(): Favorite = Favorite(
    id = id,
    placeId = placeId,
    title = title,
    address = address,
    content = content,
    rating = rating,
    isFavorite = isFavorite,
    color = color,
    city = city,
    latitude = latitude,
    longitude = longitude,
    placeType = placeType,
    phoneNumber = phoneNumber,
    googlePrimaryType = googlePrimaryType,
)

fun Favorite.toEntity(): FavoriteEntity = FavoriteEntity(
    id = id,
    placeId = placeId,
    title = title,
    address = address,
    content = content,
    rating = rating,
    isFavorite = isFavorite,
    color = color,
    city = city,
    latitude = latitude,
    longitude = longitude,
    placeType = placeType,
    phoneNumber = phoneNumber,
    googlePrimaryType = googlePrimaryType,
)
