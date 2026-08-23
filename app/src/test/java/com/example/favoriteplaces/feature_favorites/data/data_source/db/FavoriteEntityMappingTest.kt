package com.example.favoriteplaces.feature_favorites.data.data_source.db

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteEntityMappingTest {
    @Test
    fun `entity round trip preserves every persisted value`() {
        val favorite = Favorite(
            id = 17,
            placeId = null,
            title = "Café Élan ☕",
            address = "1 Unicode Way, Montréal, QC",
            content = null,
            rating = null,
            isFavorite = true,
            color = -8_266_006,
            city = "Montréal",
            latitude = 45.5019,
            longitude = -73.5674,
        )

        assertEquals(favorite, favorite.toEntity().toDomain())
    }
}
