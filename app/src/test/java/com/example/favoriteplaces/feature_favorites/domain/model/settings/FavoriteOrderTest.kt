package com.example.favoriteplaces.feature_favorites.domain.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteOrderTest {
    @Test
    fun `all order values round trip through stable storage text`() {
        val values = listOf(
            FavoriteOrder.City(OrderType.Ascending),
            FavoriteOrder.Color(OrderType.Descending),
            FavoriteOrder.IsFavorite(OrderType.Ascending),
            FavoriteOrder.Rating(OrderType.Descending),
        )
        values.forEach { assertEquals(it, FavoriteOrder.fromStorageValue(it.storageValue())) }
    }

    @Test
    fun `unknown or malformed storage text is rejected`() {
        listOf(null, "", "City", "Unknown_Ascending", "City_Sideways", "City_Ascending_extra")
            .forEach { assertNull(FavoriteOrder.fromStorageValue(it)) }
    }
}
