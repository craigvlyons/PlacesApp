package com.example.favoriteplaces.feature_favorites.presentation.citymap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DirectionsLauncherTest {
    @Test
    fun `directions URI uses exact coordinates and optional Google place id`() {
        val url = googleMapsDirectionsUrl(
            latitude = 39.7631,
            longitude = -105.0201,
            placeId = "place/id with spaces",
        )

        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=" +
                "39.7631%2C-105.0201&destination_place_id=place%2Fid+with+spaces",
            url,
        )
    }

    @Test
    fun `blank Google place id is omitted`() {
        val url = googleMapsDirectionsUrl(1.0, 2.0, " ")

        assertEquals("https://www.google.com/maps/dir/?api=1&destination=1.0%2C2.0", url)
    }

    @Test
    fun `invalid coordinates are rejected before an intent can be created`() {
        assertThrows(IllegalArgumentException::class.java) {
            googleMapsDirectionsUrl(Double.NaN, 2.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            googleMapsDirectionsUrl(1.0, 181.0)
        }
    }
}
