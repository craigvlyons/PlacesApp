package com.example.favoriteplaces.feature_favorites.data.models.predicition


import com.google.gson.annotations.SerializedName

data class StructuredFormatting(
    @SerializedName("main_text")
    val mainText: String,
    @SerializedName("main_text_matched_substrings")
    val mainTextMatchedSubstrings: List<MainTextMatchedSubstring>,
    @SerializedName("secondary_text")
    val secondaryText: String
)