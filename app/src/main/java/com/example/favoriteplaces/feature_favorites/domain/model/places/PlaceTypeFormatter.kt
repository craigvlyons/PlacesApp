package com.example.favoriteplaces.feature_favorites.domain.model.places

import java.util.Locale

object PlaceTypeFormatter {
    const val MAX_USER_LENGTH = 80

    fun fromGoogleTypes(types: Iterable<String?>): String? = types
        .mapNotNull { it?.trim()?.lowercase(Locale.US)?.takeIf(String::isNotEmpty) }
        .firstOrNull { it !in GENERIC_TYPES }
        ?.replace('_', ' ')
        ?.replaceFirstChar(Char::titlecase)

    fun normalizeUserValue(value: String): String? = value
        .trim()
        .splitToSequence(' ', '\t', '\n', '\r')
        .filter(String::isNotEmpty)
        .joinToString(" ")
        .takeIf(String::isNotEmpty)

    private val GENERIC_TYPES = setOf(
        "address",
        "establishment",
        "food",
        "geocode",
        "point_of_interest",
        "place_of_worship",
        "political",
        "premise",
        "route",
        "store",
        "street_address",
        "subpremise",
    )
}
