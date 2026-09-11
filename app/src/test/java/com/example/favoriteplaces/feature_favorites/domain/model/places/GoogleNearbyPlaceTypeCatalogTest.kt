package com.example.favoriteplaces.feature_favorites.domain.model.places

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleNearbyPlaceTypeCatalogTest {
    @Test
    fun `catalog contains only unique single filter Table A choices`() {
        val all = GoogleNearbyPlaceTypeCatalog.all

        assertEquals(all.size, all.map(NearbyCategory::storageId).distinct().size)
        assertTrue(all.all { it.includedTypes == listOf(it.storageId) })
        assertNull(GoogleNearbyPlaceTypeCatalog.find("establishment"))
        assertNull(GoogleNearbyPlaceTypeCatalog.find("food"))
        assertFalse(all.any { it.storageId == NearbyCategory.ALL_STORAGE_ID })
    }

    @Test
    fun `search accepts friendly aliases and ranks exact intent first`() {
        assertEquals("taco_restaurant", GoogleNearbyPlaceTypeCatalog.search("tacos").first().storageId)
        assertEquals("coffee_shop", GoogleNearbyPlaceTypeCatalog.search("coffee").first().storageId)
        assertEquals("steak_house", GoogleNearbyPlaceTypeCatalog.search("steakhouse").first().storageId)
        assertTrue(GoogleNearbyPlaceTypeCatalog.search("brew pub").any { it.storageId == "brewpub" })
    }

    @Test
    fun `picker normalization preserves order while rejecting stale duplicate and excess IDs`() {
        val tooMany = buildList {
            add("restaurant")
            add("not_a_google_type")
            add("restaurant")
            addAll(GoogleNearbyPlaceTypeCatalog.all.map(NearbyCategory::storageId))
        }
        val normalized = GoogleNearbyPlaceTypeCatalog.normalizePickerTypeIds(tooMany)

        assertEquals("restaurant", normalized.first())
        assertEquals(normalized.distinct(), normalized)
        assertEquals(GoogleNearbyPlaceTypeCatalog.MAX_PICKER_TYPES, normalized.size)
        assertFalse("not_a_google_type" in normalized)
    }

    @Test
    fun `legacy fixed category storage values restore to current Google types`() {
        assertEquals("restaurant", NearbyCategory.fromStorageValue("Restaurants").storageId)
        assertEquals("coffee_shop", NearbyCategory.fromStorageValue("Coffee").storageId)
        assertEquals("bar", NearbyCategory.fromStorageValue("Bars").storageId)
        assertEquals("bakery", NearbyCategory.fromStorageValue("Bakeries").storageId)
        assertEquals(NearbyCategory.All, NearbyCategory.fromStorageValue("unknown_old_value"))
    }
}
