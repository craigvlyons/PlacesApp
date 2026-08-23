package com.example.favoriteplaces.feature_favorites.data.repository

import android.os.SystemClock
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceAddressComponent
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchOrigin
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceTypeFormatter
import com.example.favoriteplaces.feature_favorites.domain.model.places.SelectedPlace
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchSession
import com.example.favoriteplaces.logging.PrivacySafeLog
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GooglePlaceSearchRepository(
    private val placesClient: PlacesClient,
) : PlaceSearchRepository {
    override fun createSession(): PlaceSearchSession {
        PrivacySafeLog.info(TAG, "Search session started")
        return GooglePlaceSearchSession(
            placesClient = placesClient,
            sessionToken = AutocompleteSessionToken.newInstance(),
        )
    }

    private companion object {
        const val TAG = "GooglePlaceSearch"
    }
}

object UnavailablePlaceSearchRepository : PlaceSearchRepository {
    override fun createSession(): PlaceSearchSession = object : PlaceSearchSession {
        private val unavailable = PlaceSearchResult.Failure(
            PlaceSearchFailure(
                category = PlaceSearchFailureCategory.Authorization,
                retryable = false,
            )
        )

        override suspend fun search(
            query: String,
            origin: PlaceSearchOrigin?,
        ): PlaceSearchResult<List<PlacePrediction>> = unavailable

        override suspend fun select(placeId: String): PlaceSearchResult<SelectedPlace> = unavailable

        override fun abandon() = Unit
    }
}

private class GooglePlaceSearchSession(
    private val placesClient: PlacesClient,
    private val sessionToken: AutocompleteSessionToken,
) : PlaceSearchSession {
    private val closed = AtomicBoolean(false)
    private val predictionsById = mutableMapOf<String, PlacePrediction>()

    override suspend fun search(
        query: String,
        origin: PlaceSearchOrigin?,
    ): PlaceSearchResult<List<PlacePrediction>> {
        val normalizedQuery = query.trim()
        if (closed.get() || normalizedQuery.length < MIN_QUERY_LENGTH) {
            return invalidRequest()
        }

        val cancellation = CancellationTokenSource()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(normalizedQuery)
            .setSessionToken(sessionToken)
            .setCancellationToken(cancellation.token)
            .apply {
                if (origin != null) {
                    val center = LatLng(origin.latitude, origin.longitude)
                    setOrigin(center)
                    setLocationBias(CircularBounds.newInstance(center, origin.radiusMeters))
                }
            }
            .build()

        PrivacySafeLog.info(
            TAG,
            "$OPERATION_AUTOCOMPLETE started (location_bias=${origin != null})"
        )
        return execute(OPERATION_AUTOCOMPLETE) {
            val predictions = placesClient.findAutocompletePredictions(request)
                .awaitWithCancellation(cancellation)
                .autocompletePredictions
                .map { prediction ->
                    PlacePrediction(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null).toString(),
                        types = prediction.types,
                        distanceMeters = prediction.distanceMeters,
                    )
                }
            predictionsById.clear()
            predictions.associateByTo(predictionsById, PlacePrediction::placeId)
            predictions
        }
    }

    override suspend fun select(placeId: String): PlaceSearchResult<SelectedPlace> {
        val normalizedPlaceId = placeId.trim()
        val prediction = predictionsById[normalizedPlaceId]
        if (
            normalizedPlaceId.isEmpty() ||
            prediction == null ||
            !closed.compareAndSet(false, true)
        ) {
            return invalidRequest()
        }

        val cancellation = CancellationTokenSource()
        val request = FetchPlaceRequest.builder(normalizedPlaceId, findPlaceDetailsFields())
            .setSessionToken(sessionToken)
            .setCancellationToken(cancellation.token)
            .build()

        PrivacySafeLog.info(TAG, "$OPERATION_DETAILS started ($DETAIL_FIELD_AUDIT)")
        return execute(OPERATION_DETAILS) {
            val place = placesClient.fetchPlace(request).awaitWithCancellation(cancellation).place
            val location = place.location
                ?: throw IncompletePlaceDataException("Selected place has no location.")
            SelectedPlace(
                placeId = normalizedPlaceId,
                displayName = prediction.primaryText,
                formattedAddress = place.formattedAddress
                    ?: throw IncompletePlaceDataException(
                        "Selected place has no formatted address."
                    ),
                latitude = location.latitude,
                longitude = location.longitude,
                addressComponents = place.addressComponents?.asList().orEmpty().map { component ->
                    PlaceAddressComponent(
                        name = component.name,
                        shortName = component.shortName,
                        types = component.types.toSet(),
                    )
                },
                thirdPartyAttributions = place.attributions.orEmpty(),
                placeType = PlaceTypeFormatter.fromGoogleTypes(
                    listOf(place.primaryType) + prediction.types
                ),
                phoneNumber = place.nationalPhoneNumber?.trim()?.takeIf(String::isNotEmpty),
                googlePrimaryType = place.primaryType?.trim()?.lowercase(Locale.US)?.takeIf(String::isNotEmpty),
            )
        }
    }

    override fun abandon() {
        if (closed.compareAndSet(false, true)) {
            PrivacySafeLog.info(TAG, "Search session abandoned")
        }
    }

    private suspend fun <T> execute(
        operation: String,
        request: suspend () -> T,
    ): PlaceSearchResult<T> {
        val startedAt = SystemClock.elapsedRealtime()
        return try {
            PlaceSearchResult.Success(request()).also {
                PrivacySafeLog.info(
                    TAG,
                    "$operation succeeded (latency=${latencyBucket(startedAt)})"
                )
            }
        } catch (exception: CancellationException) {
            PrivacySafeLog.info(TAG, "$operation cancelled")
            throw exception
        } catch (exception: Exception) {
            val failure = exception.toPlaceSearchFailure()
            PrivacySafeLog.error(
                TAG,
                "$operation failed (${failure.category}, retryable=${failure.retryable}, " +
                    "latency=${latencyBucket(startedAt)})",
                exception,
            )
            PlaceSearchResult.Failure(failure)
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

    private fun invalidRequest(): PlaceSearchResult.Failure = PlaceSearchResult.Failure(
        PlaceSearchFailure(
            category = PlaceSearchFailureCategory.InvalidRequest,
            retryable = false,
        )
    )

    private companion object {
        const val TAG = "GooglePlaceSearch"
        const val OPERATION_AUTOCOMPLETE = "Autocomplete request"
        const val OPERATION_DETAILS = "Place details request"
        const val MIN_QUERY_LENGTH = 2
        const val DETAIL_FIELD_AUDIT = "sku=Enterprise, fields=5"

    }
}

internal fun findPlaceDetailsFields(): List<Place.Field> = listOf(
    Place.Field.FORMATTED_ADDRESS,
    Place.Field.LOCATION,
    Place.Field.ADDRESS_COMPONENTS,
    Place.Field.NATIONAL_PHONE_NUMBER,
    Place.Field.PRIMARY_TYPE,
)

internal fun Throwable.toPlaceSearchFailure(): PlaceSearchFailure {
    val category = when {
        this is IOException -> PlaceSearchFailureCategory.Network
        this is IncompletePlaceDataException -> PlaceSearchFailureCategory.Unavailable
        this is IllegalArgumentException ->
            PlaceSearchFailureCategory.InvalidRequest
        this is ApiException -> when (statusCode) {
            PlacesStatusCodes.INVALID_REQUEST -> PlaceSearchFailureCategory.InvalidRequest
            PlacesStatusCodes.REQUEST_DENIED,
            CommonStatusCodes.SIGN_IN_REQUIRED,
            -> PlaceSearchFailureCategory.Authorization
            PlacesStatusCodes.OVER_QUERY_LIMIT -> PlaceSearchFailureCategory.Quota
            PlacesStatusCodes.NOT_FOUND -> PlaceSearchFailureCategory.NotFound
            CommonStatusCodes.NETWORK_ERROR,
            CommonStatusCodes.API_NOT_CONNECTED,
            CommonStatusCodes.TIMEOUT,
            -> PlaceSearchFailureCategory.Network
            CommonStatusCodes.INTERNAL_ERROR,
            CommonStatusCodes.INTERRUPTED,
            -> PlaceSearchFailureCategory.Unavailable
            else -> PlaceSearchFailureCategory.Unknown
        }
        else -> PlaceSearchFailureCategory.Unknown
    }
    return PlaceSearchFailure(
        category = category,
        retryable = category in setOf(
            PlaceSearchFailureCategory.Network,
            PlaceSearchFailureCategory.Quota,
            PlaceSearchFailureCategory.Unavailable,
            PlaceSearchFailureCategory.Unknown,
        ),
    )
}

internal class IncompletePlaceDataException(message: String) : IllegalStateException(message)

private suspend fun <T> Task<T>.awaitWithCancellation(
    cancellation: CancellationTokenSource,
): T = try {
    await()
} catch (exception: CancellationException) {
    cancellation.cancel()
    throw exception
}
