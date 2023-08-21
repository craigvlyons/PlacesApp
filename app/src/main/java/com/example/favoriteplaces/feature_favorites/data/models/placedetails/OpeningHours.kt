package com.example.favoriteplaces.feature_favorites.data.models.placedetails


import com.google.gson.annotations.SerializedName

data class OpeningHours(
    @SerializedName("open_now")
    val openNow: Boolean,
    @SerializedName("periods")
    val periods: List<PeriodX>,
    @SerializedName("weekday_text")
    val weekdayText: List<String>
)