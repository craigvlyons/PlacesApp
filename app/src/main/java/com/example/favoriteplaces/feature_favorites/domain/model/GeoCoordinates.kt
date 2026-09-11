package com.example.favoriteplaces.feature_favorites.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
    }
}

/** Returns the great-circle distance between two validated coordinates. */
fun GeoCoordinates.distanceMilesTo(other: GeoCoordinates): Double {
    val latitudeDelta = Math.toRadians(other.latitude - latitude)
    val longitudeDelta = Math.toRadians(other.longitude - longitude)
    val startLatitude = Math.toRadians(latitude)
    val endLatitude = Math.toRadians(other.latitude)
    val haversine = sin(latitudeDelta / 2).let { it * it } +
        cos(startLatitude) * cos(endLatitude) *
        sin(longitudeDelta / 2).let { it * it }
    val boundedHaversine = haversine.coerceIn(0.0, 1.0)
    return EARTH_RADIUS_MILES * 2 * atan2(
        sqrt(boundedHaversine),
        sqrt(1 - boundedHaversine),
    )
}

private const val EARTH_RADIUS_MILES = 3_958.8
