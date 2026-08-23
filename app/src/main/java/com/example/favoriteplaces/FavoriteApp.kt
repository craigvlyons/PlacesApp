package com.example.favoriteplaces

import android.app.Application
import com.example.favoriteplaces.logging.PrivacySafeLog
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FavoriteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank() || apiKey == DEFAULT_API_KEY) {
            PrivacySafeLog.error(
                TAG,
                "Places SDK is unavailable because no API key is configured",
                IllegalStateException("Missing Google API configuration")
            )
            return
        }

        if (!Places.isInitialized()) {
            try {
                Places.initializeWithNewPlacesApiEnabled(this, apiKey)
            } catch (exception: RuntimeException) {
                PrivacySafeLog.error(TAG, "Places SDK initialization failed", exception)
            }
        }
    }

    private companion object {
        const val DEFAULT_API_KEY = "DEFAULT_API_KEY"
        const val TAG = "FavoriteApp"
    }
}
