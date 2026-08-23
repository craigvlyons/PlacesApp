package com.example.favoriteplaces.feature_favorites.data.repository

import com.google.android.libraries.places.api.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FindPlaceDetailsContractTest {
    @Test
    fun `terminating details request includes durable phone and Google type metadata`() {
        val fields = findPlaceDetailsFields()

        assertEquals(
            listOf(
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.NATIONAL_PHONE_NUMBER,
                Place.Field.PRIMARY_TYPE,
            ),
            fields,
        )
        assertFalse(Place.Field.TYPES in fields)
    }

    @Test
    fun `saved place refresh requests only reviewable Google owned fields`() {
        assertEquals(
            listOf(
                Place.Field.ID,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.NATIONAL_PHONE_NUMBER,
                Place.Field.PRIMARY_TYPE,
            ),
            refreshFields(),
        )
        assertFalse(Place.Field.DISPLAY_NAME in refreshFields())
        assertFalse(Place.Field.RATING in refreshFields())
    }

    @Test
    fun `saved card action lookup requests only phone and primary type`() {
        assertEquals(
            listOf(
                Place.Field.NATIONAL_PHONE_NUMBER,
                Place.Field.PRIMARY_TYPE,
            ),
            actionMetadataFields(),
        )
    }
}
