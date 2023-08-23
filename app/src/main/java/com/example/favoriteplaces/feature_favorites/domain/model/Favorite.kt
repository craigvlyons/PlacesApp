package com.example.favoriteplaces.feature_favorites.domain.model


import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.favoriteplaces.ui.theme.BabyBlue
import com.example.favoriteplaces.ui.theme.LightGreen
import com.example.favoriteplaces.ui.theme.RedOrange
import com.example.favoriteplaces.ui.theme.RedPink
import com.example.favoriteplaces.ui.theme.Violet
import com.example.favoriteplaces.ui.theme.primary400

@Entity
data class Favorite(
    @PrimaryKey
    val id : Int? = null,
    val placeId : String? = "",
    val title: String,
    val address: String,
    val content: String?,
    val rating: Int?,
    val isFavorite: Boolean = false,
    val color: Int = favoriteColors[4].toArgb() ,
    val latitude: Double,
    val longitude: Double
){
    companion object {
        val favoriteColors = listOf(primary400, RedOrange, LightGreen, Violet, BabyBlue, RedPink)
    }
}

class InvalidNoteException(message:String): Exception(message)
