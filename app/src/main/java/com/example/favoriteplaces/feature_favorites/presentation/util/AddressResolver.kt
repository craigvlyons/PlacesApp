package com.example.favoriteplaces.feature_favorites.presentation.util

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface AddressResolver {
    suspend fun resolve(latitude: Double, longitude: Double): String?
}

class AndroidAddressResolver(
    private val geocoder: Geocoder
) : AddressResolver {
    override suspend fun resolve(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolveAsync(latitude, longitude)
        } else {
            resolveBlocking(latitude, longitude)
        }
        return address?.getAddressLine(0)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun resolveAsync(latitude: Double, longitude: Double): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IOException(errorMessage ?: "Geocoder failed without details")
                            )
                        }
                    }
                }
            )
        }

    @Suppress("DEPRECATION")
    private suspend fun resolveBlocking(latitude: Double, longitude: Double): Address? =
        withContext(Dispatchers.IO) {
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }
}
