package com.example.favoriteplaces.feature_favorites.data.repository

import android.os.SystemClock
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceAddressComponent
import com.example.favoriteplaces.feature_favorites.domain.repository.GooglePlaceDetails
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshResult
import com.example.favoriteplaces.feature_favorites.presentation.util.PlaceCityResolver
import com.example.favoriteplaces.logging.PrivacySafeLog
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Locale

class GoogleSavedPlaceRefreshRepository(
    private val placesClient: PlacesClient,
) : SavedPlaceRefreshRepository {
    override suspend fun refresh(placeId: String): SavedPlaceRefreshResult {
        val normalizedId = placeId.trim()
        if (normalizedId.isEmpty()) {
            return SavedPlaceRefreshResult.Failure(
                IllegalArgumentException("Blank place ID").toPlaceSearchFailure()
            )
        }

        val cancellation = CancellationTokenSource()
        val request = FetchPlaceRequest.builder(normalizedId, refreshFields())
            .setCancellationToken(cancellation.token)
            .build()
        val startedAt = SystemClock.elapsedRealtime()
        PrivacySafeLog.info(TAG, "User-requested details refresh started ($FIELD_AUDIT)")

        return try {
            val place = placesClient.fetchPlace(request).await().place
            val location = place.location
                ?: throw IncompletePlaceDataException("Refreshed place has no location.")
            val address = place.formattedAddress?.trim()?.takeIf(String::isNotEmpty)
                ?: throw IncompletePlaceDataException("Refreshed place has no address.")
            val components = place.addressComponents?.asList().orEmpty().map { component ->
                PlaceAddressComponent(
                    name = component.name,
                    shortName = component.shortName,
                    types = component.types.toSet(),
                )
            }
            val city = PlaceCityResolver.resolve(components)
                ?: throw IncompletePlaceDataException("Refreshed place has no city.")
            val canonicalId = place.id?.trim()?.takeIf(String::isNotEmpty) ?: normalizedId
            SavedPlaceRefreshResult.Success(
                GooglePlaceDetails(
                    placeId = canonicalId,
                    address = address,
                    city = city,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    phoneNumber = place.nationalPhoneNumber?.trim()?.takeIf(String::isNotEmpty),
                    googlePrimaryType = place.primaryType?.trim()?.lowercase(Locale.US)?.takeIf(String::isNotEmpty),
                )
            ).also {
                PrivacySafeLog.info(
                    TAG,
                    "User-requested details refresh succeeded (${latencyBucket(startedAt)})"
                )
            }
        } catch (exception: CancellationException) {
            cancellation.cancel()
            PrivacySafeLog.info(TAG, "User-requested details refresh cancelled")
            throw exception
        } catch (exception: Exception) {
            val failure = exception.toPlaceSearchFailure()
            PrivacySafeLog.error(
                TAG,
                "User-requested details refresh failed (${failure.category}, " +
                    "retryable=${failure.retryable}, ${latencyBucket(startedAt)})",
                exception,
            )
            SavedPlaceRefreshResult.Failure(failure)
        }
    }

    private fun latencyBucket(startedAt: Long): String = when (
        SystemClock.elapsedRealtime() - startedAt
    ) {
        in 0..<250 -> "under_250ms"
        in 250..<1_000 -> "250_to_999ms"
        in 1_000..<3_000 -> "1_to_3s"
        else -> "over_3s"
    }

    private companion object {
        const val TAG = "GooglePlaceRefresh"
        const val FIELD_AUDIT = "sku=Enterprise, fields=6"
    }
}

object UnavailableSavedPlaceRefreshRepository : SavedPlaceRefreshRepository {
    override suspend fun refresh(placeId: String): SavedPlaceRefreshResult =
        SavedPlaceRefreshResult.Failure(
            com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure(
                category = com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory.Authorization,
                retryable = false,
            )
        )
}

internal fun refreshFields(): List<Place.Field> = listOf(
    Place.Field.ID,
    Place.Field.FORMATTED_ADDRESS,
    Place.Field.LOCATION,
    Place.Field.ADDRESS_COMPONENTS,
    Place.Field.NATIONAL_PHONE_NUMBER,
    Place.Field.PRIMARY_TYPE,
)
