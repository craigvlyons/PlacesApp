package com.example.favoriteplaces.feature_favorites.data.models.predicition


import com.google.gson.annotations.SerializedName

data class Term(
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("value")
    val value: String
)