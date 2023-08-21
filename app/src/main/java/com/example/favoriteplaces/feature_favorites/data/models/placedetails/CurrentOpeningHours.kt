package com.example.favoriteplaces.feature_favorites.data.models.placedetails


import com.google.gson.annotations.SerializedName

data class CurrentOpeningHours(
    @SerializedName("open_now")
    val openNow: Boolean,
    @SerializedName("periods")
    val periods: List<Period>,
    @SerializedName("weekday_text")
    val weekdayText: List<String>
)