package com.example.favoriteplaces.feature_favorites.domain.model.places

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceReservationEligibilityTest {
    @Test
    fun restaurantPrimaryTypesAreEligible() {
        assertTrue(isReservationEligibleGoogleType("restaurant"))
        assertTrue(isReservationEligibleGoogleType("mexican_restaurant"))
        assertTrue(isReservationEligibleGoogleType("fine_dining_restaurant"))
        assertTrue(isReservationEligibleGoogleType("steak_house"))
        assertTrue(isReservationEligibleGoogleType("bistro"))
    }

    @Test
    fun editableDisplayLabelsAndNonrestaurantsDoNotDriveEligibility() {
        assertFalse(isReservationEligibleGoogleType(null))
        assertFalse(isReservationEligibleGoogleType(""))
        assertFalse(isReservationEligibleGoogleType("cafe"))
        assertFalse(isReservationEligibleGoogleType("bakery"))
        assertFalse(isReservationEligibleGoogleType("Restaurant with patio"))
    }
}
