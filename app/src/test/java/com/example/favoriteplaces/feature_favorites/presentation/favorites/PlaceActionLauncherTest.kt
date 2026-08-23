package com.example.favoriteplaces.feature_favorites.presentation.favorites

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceActionLauncherTest {
    @Test
    fun dialUriPreservesHumanReadablePhoneForTheSystemDialer() {
        assertEquals("tel:+13035550100", dialUriString("+1 (303) 555-0100"))
        assertNull(dialUriString("no number"))
    }

    @Test
    fun openTableSearchEncodesTheRestaurantNameWithoutOtherPrivateFields() {
        assertEquals(
            "https://www.opentable.com/s?covers=2&term=Root%20Down%20%26%20Grill",
            openTableSearchUrl("Root Down & Grill"),
        )
    }

    @Test
    fun googleSearchUsesOnlyTheSavedPlaceNameAndAddress() {
        assertEquals(
            "https://www.google.com/search?q=Root%20Down%20%26%20Grill%201600%20W%2033rd%20Ave%2C%20Denver%2C%20CO",
            googlePlaceSearchUrl(
                placeName = " Root Down & Grill ",
                address = "1600 W 33rd Ave, Denver, CO",
            ),
        )
        assertNull(googlePlaceSearchUrl(" ", ""))
    }
}
