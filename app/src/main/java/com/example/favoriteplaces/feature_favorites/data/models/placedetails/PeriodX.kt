package com.example.favoriteplaces.feature_favorites.data.models.placedetails


import com.google.gson.annotations.SerializedName

data class PeriodX(
    @SerializedName("close")
    val close: CloseX,
    @SerializedName("open")
    val `open`: OpenX
)