package com.example.favoriteplaces.feature_favorites.data.models.placedetails


import com.google.gson.annotations.SerializedName

data class CloseX(
    @SerializedName("day")
    val day: Int,
    @SerializedName("time")
    val time: String
)