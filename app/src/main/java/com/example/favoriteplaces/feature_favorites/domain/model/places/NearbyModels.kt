package com.example.favoriteplaces.feature_favorites.domain.model.places

import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates

data class NearbyCategory(
    val storageId: String,
    val label: String,
    val includedTypes: List<String>,
) {
    init {
        require(storageId.isNotBlank()) { "A nearby category requires a storage ID." }
        require(label.isNotBlank()) { "A nearby category requires a label." }
        require(includedTypes.size <= MAX_INCLUDED_TYPES) {
            "A nearby category cannot exceed Google's Nearby type limit."
        }
    }

    companion object {
        const val ALL_STORAGE_ID = "__all__"
        private const val MAX_INCLUDED_TYPES = 50

        val All = NearbyCategory(ALL_STORAGE_ID, "Any type", emptyList())
        val Restaurants get() = GoogleNearbyPlaceTypeCatalog.require("restaurant")
        val Coffee get() = GoogleNearbyPlaceTypeCatalog.require("coffee_shop")
        val Bars get() = GoogleNearbyPlaceTypeCatalog.require("bar")
        val Bakeries get() = GoogleNearbyPlaceTypeCatalog.require("bakery")

        fun fromStorageValue(value: String?): NearbyCategory = when (value) {
            null, ALL_STORAGE_ID, "All" -> All
            "Restaurants" -> Restaurants
            "Coffee" -> Coffee
            "Bars" -> Bars
            "Bakeries" -> Bakeries
            else -> GoogleNearbyPlaceTypeCatalog.find(value) ?: All
        }
    }
}

data class NearbyPlace(
    val placeId: String,
    val displayName: String,
    val formattedAddress: String,
    val coordinates: GeoCoordinates,
    val primaryType: String?,
    val addressComponents: List<PlaceAddressComponent>,
    val thirdPartyAttributions: List<String>,
    val distanceMiles: Double,
)

data class NearbySearchRequest(
    val origin: GeoCoordinates,
    val radiusMiles: Int,
    val category: NearbyCategory,
) {
    init {
        require(radiusMiles in 1..31) { "Radius must be between 1 and 31 miles." }
    }
}
