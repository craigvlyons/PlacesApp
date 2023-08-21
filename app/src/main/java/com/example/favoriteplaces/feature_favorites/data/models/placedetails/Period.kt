package com.example.favoriteplaces.feature_favorites.data.models.placedetails


import com.google.gson.annotations.SerializedName

data class Period(
    @SerializedName("close")
    val close: Close,
    @SerializedName("open")
    val `open`: Open
)