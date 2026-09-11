package com.example.favoriteplaces.feature_favorites.data.repository

import android.os.SystemClock
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.distanceMilesTo
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyPlace
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbySearchRequest as AppNearbySearchRequest
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceAddressComponent
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult
import com.example.favoriteplaces.feature_favorites.domain.repository.NearbyPlacesRepository
import com.example.favoriteplaces.logging.PrivacySafeLog
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest as SdkNearbySearchRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleNearbyPlacesRepository(
    private val placesClient: PlacesClient,
) : NearbyPlacesRepository {
    override suspend fun search(
        request: AppNearbySearchRequest,
    ): PlaceSearchResult<List<NearbyPlace>> {
        val cancellation = CancellationTokenSource()
        val sdkRequest = buildNearbySdkRequest(request, cancellation)
        val started = SystemClock.elapsedRealtime()
        PrivacySafeLog.info(
            TAG,
            "Nearby request started (fields=${FIELDS.size}, radius_bucket=${request.radiusMiles}, " +
                "type_filter=${request.category != com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyCategory.All})"
        )
        return try {
            val responsePlaces = placesClient.searchNearby(sdkRequest).await().places
            val places = responsePlaces.mapNotNull { place ->
                val id = place.id
                val name = place.displayName
                val address = place.formattedAddress
                val location = place.location
                if (id == null || name == null || address == null || location == null) return@mapNotNull null
                NearbyPlace(
                    placeId = id,
                    displayName = name,
                    formattedAddress = address,
                    coordinates = GeoCoordinates(location.latitude, location.longitude),
                    primaryType = place.primaryType,
                    addressComponents = place.addressComponents?.asList().orEmpty().map { component ->
                        PlaceAddressComponent(component.name, component.shortName, component.types.toSet())
                    },
                    thirdPartyAttributions = place.attributions.orEmpty(),
                    distanceMiles = request.origin.distanceMilesTo(
                        GeoCoordinates(location.latitude, location.longitude)
                    ),
                )
            }
            val discardedCount = responsePlaces.size - places.size
            if (discardedCount > 0) {
                PrivacySafeLog.info(
                    TAG,
                    "Nearby response omitted incomplete records " +
                        "(count_bucket=${discardedCount.coerceAtMost(MAX_RESULTS)})"
                )
            }
            if (responsePlaces.isNotEmpty() && places.isEmpty()) {
                PrivacySafeLog.info(TAG, "Nearby response contained no usable records")
                return PlaceSearchResult.Failure(
                    PlaceSearchFailure(
                        PlaceSearchFailureCategory.Unavailable,
                        retryable = true,
                    )
                )
            }
            PrivacySafeLog.info(
                TAG,
                "Nearby request succeeded (count_bucket=${places.size.coerceAtMost(MAX_RESULTS)}, " +
                    "latency=${latencyBucket(started)})"
            )
            PlaceSearchResult.Success(places)
        } catch (exception: CancellationException) {
            cancellation.cancel()
            PrivacySafeLog.info(TAG, "Nearby request cancelled")
            throw exception
        } catch (exception: Exception) {
            val failure = exception.toPlaceSearchFailure()
            PrivacySafeLog.error(
                TAG,
                "Nearby request failed (${failure.category}, retryable=${failure.retryable}, " +
                    "latency=${latencyBucket(started)})",
                exception,
            )
            PlaceSearchResult.Failure(failure)
        }
    }

    private fun latencyBucket(started: Long): String = when (SystemClock.elapsedRealtime() - started) {
        in 0..<500 -> "under_500ms"
        in 500..<2_000 -> "500ms_to_2s"
        else -> "over_2s"
    }

    internal companion object {
        const val TAG = "GoogleNearby"
        const val MAX_RESULTS = 20
        const val METERS_PER_MILE = 1_609.344
        val FIELDS = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.PRIMARY_TYPE,
            Place.Field.ADDRESS_COMPONENTS,
        )
    }
}

internal fun buildNearbySdkRequest(
    request: AppNearbySearchRequest,
    cancellation: CancellationTokenSource,
): SdkNearbySearchRequest {
    val center = LatLng(request.origin.latitude, request.origin.longitude)
    return SdkNearbySearchRequest.builder(
        CircularBounds.newInstance(center, request.radiusMiles * GoogleNearbyPlacesRepository.METERS_PER_MILE),
        GoogleNearbyPlacesRepository.FIELDS,
    )
        .setMaxResultCount(GoogleNearbyPlacesRepository.MAX_RESULTS)
        .setRankPreference(SdkNearbySearchRequest.RankPreference.DISTANCE)
        .setCancellationToken(cancellation.token)
        .apply {
            if (request.category.includedTypes.isNotEmpty()) {
                setIncludedTypes(request.category.includedTypes)
            }
        }
        .build()
}

object UnavailableNearbyPlacesRepository : NearbyPlacesRepository {
    override suspend fun search(request: AppNearbySearchRequest) = PlaceSearchResult.Failure(
        PlaceSearchFailure(PlaceSearchFailureCategory.Authorization, retryable = false)
    )
}
