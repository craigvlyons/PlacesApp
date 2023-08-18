package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import com.google.android.gms.maps.model.LatLng


data class AutocompleteResult(
    val address: String,
    val placeId: String,
    val location: LatLng,
//    val latitude: Double,  // Example: Latitude coordinate
//    val longitude: Double, // Example: Longitude coordinate
//    val types: List<Any>
)
