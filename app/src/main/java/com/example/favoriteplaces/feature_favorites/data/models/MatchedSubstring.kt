package com.example.favoriteplaces.feature_favorites.data.models


import com.google.gson.annotations.SerializedName

data class MatchedSubstring(
    @SerializedName("length")
    val length: Int,
    @SerializedName("offset")
    val offset: Int
)