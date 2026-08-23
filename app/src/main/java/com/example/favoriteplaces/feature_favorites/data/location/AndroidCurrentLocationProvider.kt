package com.example.favoriteplaces.feature_favorites.data.location

import android.annotation.SuppressLint
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.repository.CurrentLocationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AndroidCurrentLocationProvider @Inject constructor(
    private val client: FusedLocationProviderClient,
) : CurrentLocationProvider {
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): GeoCoordinates? {
        val cancellation = CancellationTokenSource()
        return try {
            client.getCurrentLocation(
                CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MILLIS)
                    .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
                    .build(),
                cancellation.token,
            ).await()?.let { location ->
                GeoCoordinates(location.latitude, location.longitude)
            }
        } catch (exception: CancellationException) {
            cancellation.cancel()
            throw exception
        }
    }

    private companion object {
        const val MAX_LOCATION_AGE_MILLIS = 2 * 60 * 1_000L
        const val LOCATION_TIMEOUT_MILLIS = 10_000L
    }
}
