package com.example.favoriteplaces.feature_favorites.presentation.citymap

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.favoriteplaces.logging.PrivacySafeLog
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun googleMapsDirectionsUrl(
    latitude: Double,
    longitude: Double,
    placeId: String? = null,
): String {
    require(latitude.isFinite() && latitude in -90.0..90.0) {
        "Directions latitude must be valid."
    }
    require(longitude.isFinite() && longitude in -180.0..180.0) {
        "Directions longitude must be valid."
    }
    return buildString {
        append("https://www.google.com/maps/dir/?api=1&destination=")
        append(urlEncode("$latitude,$longitude"))
        placeId?.takeIf(String::isNotBlank)?.let {
            append("&destination_place_id=")
            append(urlEncode(it))
        }
    }
}

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())

fun openGoogleDirections(
    context: Context,
    latitude: Double,
    longitude: Double,
    placeId: String? = null,
): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, googleMapsDirectionsUrl(latitude, longitude, placeId).toUri())
    )
    true
} catch (exception: ActivityNotFoundException) {
    PrivacySafeLog.error(TAG, "No directions handler is installed", exception)
    false
} catch (exception: SecurityException) {
    PrivacySafeLog.error(TAG, "Directions launch was blocked", exception)
    false
} catch (exception: IllegalArgumentException) {
    PrivacySafeLog.error(TAG, "Directions coordinates were invalid", exception)
    false
}

private const val TAG = "DirectionsLauncher"
