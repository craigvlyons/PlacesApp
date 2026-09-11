package com.example.favoriteplaces.feature_favorites.data.repository

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSearchFailureMappingTest {
    @Test
    fun `request denied is a non-retryable authorization failure`() {
        val failure = ApiException(Status(PlacesStatusCodes.REQUEST_DENIED))
            .toPlaceSearchFailure()

        assertEquals(PlaceSearchFailureCategory.Authorization, failure.category)
        assertFalse(failure.retryable)
    }

    @Test
    fun `over query limit is a retryable quota failure`() {
        val failure = ApiException(Status(PlacesStatusCodes.OVER_QUERY_LIMIT))
            .toPlaceSearchFailure()

        assertEquals(PlaceSearchFailureCategory.Quota, failure.category)
        assertTrue(failure.retryable)
    }

    @Test
    fun `network failures remain retryable without provider details`() {
        val failure = IOException("private request details").toPlaceSearchFailure()

        assertEquals(PlaceSearchFailureCategory.Network, failure.category)
        assertTrue(failure.retryable)
    }

    @Test
    fun `incomplete selected data is retryable unavailable`() {
        val failure = IncompletePlaceDataException("private place details")
            .toPlaceSearchFailure()

        assertEquals(PlaceSearchFailureCategory.Unavailable, failure.category)
        assertTrue(failure.retryable)
    }

    @Test
    fun `invalid client requests are not retryable`() {
        val failure = IllegalArgumentException("private query")
            .toPlaceSearchFailure()

        assertEquals(PlaceSearchFailureCategory.InvalidRequest, failure.category)
        assertFalse(failure.retryable)
    }

    @Test
    fun `unexpected failures retain a retryable unknown category`() {
        val failure = IllegalStateException("private provider response")
            .toPlaceSearchFailure()

        assertEquals(PlaceSearchFailureCategory.Unknown, failure.category)
        assertTrue(failure.retryable)
    }
}
