package com.example.favoriteplaces.feature_favorites.domain.model

import com.google.android.gms.maps.model.LatLng


data class MapItem(
    val itemPosition: LatLng,
    val itemTitle: String,
    val itemSnippet: String,
) {
    constructor(latitude: Double, longitude: Double, title: String) :
            this(LatLng(latitude, longitude), title, title)

    fun getPosition(): LatLng =
        itemPosition

    fun getTitle(): String =
        itemTitle

    fun getSnippet(): String =
        itemSnippet
}


