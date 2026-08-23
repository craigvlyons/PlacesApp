package com.example.favoriteplaces.feature_favorites.domain.model.places

import java.util.Locale

fun isReservationEligibleGoogleType(googlePrimaryType: String?): Boolean {
    val type = googlePrimaryType?.trim()?.lowercase(Locale.US)?.takeIf(String::isNotEmpty)
        ?: return false
    return type == "restaurant" ||
        type.endsWith("_restaurant") ||
        type in OTHER_RESERVATION_RESTAURANT_TYPES
}

private val OTHER_RESERVATION_RESTAURANT_TYPES = setOf(
    "bar_and_grill",
    "bistro",
    "diner",
    "gastropub",
    "steak_house",
)
