package com.example.favoriteplaces.feature_favorites.domain.model

import com.google.android.gms.maps.model.LatLng


data class MapItem(
    val itemPosition: LatLng,
    val itemTitle: String,
    val itemSnippet: String,
) {
    fun getPosition(): LatLng =
        itemPosition

    fun getTitle(): String =
        itemTitle

    fun getSnippet(): String =
        itemSnippet
}


