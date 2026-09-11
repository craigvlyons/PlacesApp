package com.example.favoriteplaces.feature_favorites.presentation.util

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceAddressComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceCityResolverTest {
    @Test
    fun structuredLocalityWinsOverCommaPosition() {
        val components = listOf(
            PlaceAddressComponent("Colorado", "CO", setOf("administrative_area_level_1")),
            PlaceAddressComponent(
                "Colorado Springs",
                "Colorado Springs",
                setOf("locality"),
            )
        )

        assertEquals(
            "Colorado Springs",
            PlaceCityResolver.resolve(components)
        )
    }

    @Test
    fun postalTownSupportsAddressesWithoutLocality() {
        val components = listOf(
            PlaceAddressComponent("London", "London", setOf("postal_town"))
        )

        assertEquals("London", PlaceCityResolver.resolve(components))
    }

    @Test
    fun missingStructuredCityDoesNotParseTheDisplayAddress() {
        assertNull(PlaceCityResolver.resolve(emptyList()))
    }
}
