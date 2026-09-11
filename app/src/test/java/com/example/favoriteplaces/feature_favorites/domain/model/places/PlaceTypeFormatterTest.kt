package com.example.favoriteplaces.feature_favorites.domain.model.places

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceTypeFormatterTest {
    @Test
    fun `specific Google type is selected and humanized without address parsing`() {
        assertEquals(
            "Mexican restaurant",
            PlaceTypeFormatter.fromGoogleTypes(
                listOf("food", "mexican_restaurant", "point_of_interest", "establishment")
            )
        )
    }

    @Test
    fun `generic blank and missing Google values stay unset`() {
        assertNull(PlaceTypeFormatter.fromGoogleTypes(listOf(null, "", "food", "establishment")))
    }

    @Test
    fun `user value trims and collapses whitespace while preserving their wording`() {
        assertEquals("Wine & tapas bar", PlaceTypeFormatter.normalizeUserValue("  Wine  &\t tapas bar  "))
        assertNull(PlaceTypeFormatter.normalizeUserValue(" \n "))
    }
}
