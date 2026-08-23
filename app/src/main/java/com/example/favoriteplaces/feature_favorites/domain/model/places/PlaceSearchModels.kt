package com.example.favoriteplaces.feature_favorites.domain.model.places

data class PlaceSearchOrigin(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
        require(radiusMeters in MIN_RADIUS_METERS..MAX_RADIUS_METERS) {
            "Radius must be between $MIN_RADIUS_METERS and $MAX_RADIUS_METERS meters."
        }
    }

    private companion object {
        const val MIN_RADIUS_METERS = 1.0
        const val MAX_RADIUS_METERS = 50_000.0
    }
}

data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val types: List<String>,
    val distanceMeters: Int?,
)

data class PlaceAddressComponent(
    val name: String,
    val shortName: String?,
    val types: Set<String>,
)

data class SelectedPlace(
    val placeId: String,
    val displayName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val addressComponents: List<PlaceAddressComponent>,
    val thirdPartyAttributions: List<String> = emptyList(),
    val placeType: String? = null,
    val phoneNumber: String? = null,
    val googlePrimaryType: String? = null,
)

enum class PlaceSearchFailureCategory {
    InvalidRequest,
    Authorization,
    Quota,
    Network,
    NotFound,
    Unavailable,
    Unknown,
}

data class PlaceSearchFailure(
    val category: PlaceSearchFailureCategory,
    val retryable: Boolean,
)

sealed interface PlaceSearchResult<out T> {
    data class Success<T>(val value: T) : PlaceSearchResult<T>
    data class Failure(val reason: PlaceSearchFailure) : PlaceSearchResult<Nothing>
}
