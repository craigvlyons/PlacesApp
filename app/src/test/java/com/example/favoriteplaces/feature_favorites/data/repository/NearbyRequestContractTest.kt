package com.example.favoriteplaces.feature_favorites.data.repository

import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbySearchRequest
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyRequestContractTest {
    @Test
    fun `request uses reviewed fields distance rank result cap radius and types`() {
        val request = buildNearbySdkRequest(
            NearbySearchRequest(GeoCoordinates(39.7, -104.9), 10, NearbyCategory.Coffee),
            CancellationTokenSource(),
        )

        assertEquals(20, request.maxResultCount)
        assertEquals(SearchNearbyRequest.RankPreference.DISTANCE, request.rankPreference)
        assertEquals(listOf("coffee_shop"), request.includedTypes)
        assertEquals(
            setOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.PRIMARY_TYPE,
                Place.Field.ADDRESS_COMPONENTS,
            ),
            request.placeFields.toSet(),
        )
        assertTrue(request.locationRestriction.toString().isNotBlank())
    }

    @Test
    fun `Any type omits Google includedTypes`() {
        val request = buildNearbySdkRequest(
            NearbySearchRequest(GeoCoordinates(39.7, -104.9), 10, NearbyCategory.All),
            CancellationTokenSource(),
        )

        assertNull(request.includedTypes)
    }
}
