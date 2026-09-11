package com.example.favoriteplaces.feature_favorites.presentation.util

import androidx.lifecycle.SavedStateHandle
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.logging.PrivacySafeLog

fun SavedStateHandle.restoreGeoCoordinates(
    latitudeKey: String,
    longitudeKey: String,
    logTag: String,
): GeoCoordinates? {
    val latitude = get<Double>(latitudeKey) ?: return null
    val longitude = get<Double>(longitudeKey) ?: return null
    return try {
        GeoCoordinates(latitude, longitude)
    } catch (exception: IllegalArgumentException) {
        PrivacySafeLog.error(logTag, "Discarded invalid restored map coordinates", exception)
        remove<Double>(latitudeKey)
        remove<Double>(longitudeKey)
        null
    }
}
