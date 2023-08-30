package com.example.favoriteplaces.feature_favorites.domain.model

import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson


data class ColorVariation(
    val color: Int,
    val favorites: List<Favorite>
){
    companion object {
        fun fromJson(json: String): ColorVariation {
            return Gson().fromJson(json, ColorVariation::class.java)
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun toMapItems(): List<MapItem> {
        return favorites.map { favorite ->
            MapItem(
                LatLng(favorite.latitude, favorite.longitude),
                itemTitle  = favorite.title,
                itemSnippet = favorite.title
            )
        }

    }

}