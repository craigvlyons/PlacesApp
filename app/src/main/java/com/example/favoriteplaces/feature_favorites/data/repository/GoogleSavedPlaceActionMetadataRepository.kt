package com.example.favoriteplaces.feature_favorites.data.repository

import android.os.SystemClock
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadata
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadataRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadataResult
import com.example.favoriteplaces.logging.PrivacySafeLog
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleSavedPlaceActionMetadataRepository(
    private val placesClient: PlacesClient,
) : SavedPlaceActionMetadataRepository {
    override suspend fun fetch(placeId: String): SavedPlaceActionMetadataResult {
        val normalizedId = placeId.trim()
        if (normalizedId.isEmpty()) {
            return SavedPlaceActionMetadataResult.Failure(
                PlaceSearchFailure(PlaceSearchFailureCategory.InvalidRequest, retryable = false)
            )
        }

        val cancellation = CancellationTokenSource()
        val request = FetchPlaceRequest.builder(normalizedId, actionMetadataFields())
            .setCancellationToken(cancellation.token)
            .build()
        val startedAt = SystemClock.elapsedRealtime()
        PrivacySafeLog.info(TAG, "On-demand saved-place actions lookup started ($FIELD_AUDIT)")

        return try {
            val place = placesClient.fetchPlace(request).await().place
            SavedPlaceActionMetadataResult.Success(
                SavedPlaceActionMetadata(
                    phoneNumber = place.nationalPhoneNumber?.trim()?.takeIf(String::isNotEmpty),
                    googlePrimaryType = place.primaryType
                        ?.trim()
                        ?.lowercase(Locale.US)
                        ?.takeIf(String::isNotEmpty),
                )
            ).also {
                PrivacySafeLog.info(
                    TAG,
                    "On-demand saved-place actions lookup succeeded (${latencyBucket(startedAt)})"
                )
            }
        } catch (exception: CancellationException) {
            cancellation.cancel()
            PrivacySafeLog.info(TAG, "On-demand saved-place actions lookup cancelled")
            throw exception
        } catch (exception: Exception) {
            val failure = exception.toPlaceSearchFailure()
            PrivacySafeLog.error(
                TAG,
                "On-demand saved-place actions lookup failed (${failure.category}, " +
                    "retryable=${failure.retryable}, ${latencyBucket(startedAt)})",
                exception,
            )
            SavedPlaceActionMetadataResult.Failure(failure)
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
        const val TAG = "GooglePlaceActions"
        const val FIELD_AUDIT = "sku=Enterprise, fields=2"
    }
}

internal fun actionMetadataFields(): List<Place.Field> = listOf(
    Place.Field.NATIONAL_PHONE_NUMBER,
    Place.Field.PRIMARY_TYPE,
)

object UnavailableSavedPlaceActionMetadataRepository : SavedPlaceActionMetadataRepository {
    override suspend fun fetch(placeId: String): SavedPlaceActionMetadataResult =
        SavedPlaceActionMetadataResult.Failure(
            PlaceSearchFailure(PlaceSearchFailureCategory.Authorization, retryable = false)
        )
}
