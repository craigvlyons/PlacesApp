package com.example.favoriteplaces.feature_favorites.domain.model

import com.example.favoriteplaces.feature_favorites.domain.model.settings.DEFAULT_CARD_COLOR_ARGB

data class Favorite(
    val id: Int? = null,
    val placeId: String? = null,
    val title: String,
    val address: String,
    val content: String?,
    val rating: Int?,
    val isFavorite: Boolean = false,
    val color: Int = DEFAULT_COLOR_ARGB,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val placeType: String? = null,
    val phoneNumber: String? = null,
    val googlePrimaryType: String? = null,
) {
    companion object {
        const val DEFAULT_COLOR_ARGB: Int = DEFAULT_CARD_COLOR_ARGB
    }
}
