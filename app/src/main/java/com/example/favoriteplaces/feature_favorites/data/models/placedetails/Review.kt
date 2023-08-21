package com.example.favoriteplaces.feature_favorites.data.models.placedetails


import com.google.gson.annotations.SerializedName

data class Review(
    @SerializedName("author_name")
    val authorName: String,
    @SerializedName("author_url")
    val authorUrl: String,
    @SerializedName("language")
    val language: String,
    @SerializedName("original_language")
    val originalLanguage: String,
    @SerializedName("profile_photo_url")
    val profilePhotoUrl: String,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("relative_time_description")
    val relativeTimeDescription: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("time")
    val time: Int,
    @SerializedName("translated")
    val translated: Boolean
)