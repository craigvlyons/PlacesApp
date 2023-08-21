package com.example.favoriteplaces.feature_favorites.data.models.placedetails


import com.google.gson.annotations.SerializedName

data class Close(
    @SerializedName("date")
    val date: String,
    @SerializedName("day")
    val day: Int,
    @SerializedName("time")
    val time: String
)