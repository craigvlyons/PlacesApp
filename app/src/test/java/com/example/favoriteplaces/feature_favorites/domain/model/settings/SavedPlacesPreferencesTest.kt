package com.example.favoriteplaces.feature_favorites.domain.model.settings

import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedPlacesPreferencesTest {
    @Test
    fun `fresh preferences use the established light blue for both defaults`() {
        val preferences = SavedPlacesPreferences()

        assertEquals(-8_266_006, preferences.appColor)
        assertEquals(-8_266_006, preferences.defaultCardColor)
        assertEquals(
            GoogleNearbyPlaceTypeCatalog.defaultPickerTypeIds,
            preferences.nearbyDiscoveryTypeIds,
        )
    }

    @Test
    fun `transparent values are rejected while opaque ARGB values are accepted`() {
        assertFalse(0.isValidArgbColor())
        assertFalse(0x00FFFFFF.isValidArgbColor())
        assertTrue((-12_816_751).isValidArgbColor())
    }
}
