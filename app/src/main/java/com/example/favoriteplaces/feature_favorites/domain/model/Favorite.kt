package com.example.favoriteplaces.feature_favorites.domain.model


import com.example.favoriteplaces.ui.theme.BabyBlue
import com.example.favoriteplaces.ui.theme.LightGreen
import com.example.favoriteplaces.ui.theme.RedOrange
import com.example.favoriteplaces.ui.theme.RedPink
import com.example.favoriteplaces.ui.theme.Violet
import com.example.favoriteplaces.ui.theme.primary400
import com.google.android.gms.maps.model.LatLng

data class Favorite(
    val id : Int? = null,
    val title: String,
    val content: String,
    val rating: Int?,
    val isFavorite: Boolean = false,
    val color: Int,
    val location: LatLng = LatLng(0.0, 0.0)
){
    companion object {
        val favoriteColors = listOf(primary400, RedOrange, LightGreen, Violet, BabyBlue, RedPink)
    }
}

class InvalidNoteException(message:String): Exception(message)