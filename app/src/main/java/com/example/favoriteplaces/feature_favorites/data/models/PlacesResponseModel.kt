package com.example.favoriteplaces.feature_favorites.data.models


import com.google.gson.annotations.SerializedName

data class PlacesResponseModel(
    @SerializedName("predictions")
    val predictions: List<Prediction>,
    @SerializedName("status")
    val status: String
)
