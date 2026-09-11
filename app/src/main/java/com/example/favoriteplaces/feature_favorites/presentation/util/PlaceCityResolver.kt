package com.example.favoriteplaces.feature_favorites.presentation.util

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceAddressComponent

object PlaceCityResolver {
    fun resolve(
        addressComponents: List<PlaceAddressComponent>,
    ): String? {
        return CITY_COMPONENT_TYPES.firstNotNullOfOrNull { desiredType ->
            addressComponents
                .firstOrNull { desiredType in it.types }
                ?.name
                ?.trim()
                ?.takeIf(String::isNotEmpty)
        }
    }

    private val CITY_COMPONENT_TYPES = listOf(
        "locality",
        "postal_town",
        "sublocality_level_1",
        "administrative_area_level_2",
        "administrative_area_level_1",
    )
}
