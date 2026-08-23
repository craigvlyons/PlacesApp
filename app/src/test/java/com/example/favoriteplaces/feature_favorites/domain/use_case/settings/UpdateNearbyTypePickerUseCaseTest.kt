package com.example.favoriteplaces.feature_favorites.domain.use_case.settings

import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.testutil.FakeSavedPlacesPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateNearbyTypePickerUseCaseTest {
    @Test
    fun `updates are validated ordered and idempotent`() = runTest {
        val repository = FakeSavedPlacesPreferencesRepository()
        val update = UpdateNearbyTypePickerUseCase(repository)

        assertEquals(NearbyTypePickerUpdateResult.Updated, update("ramen_restaurant", add = true))
        assertEquals(NearbyTypePickerUpdateResult.Unchanged, update("ramen_restaurant", add = true))
        assertEquals(NearbyTypePickerUpdateResult.Unsupported, update("not_a_type", add = true))
        assertEquals(
            GoogleNearbyPlaceTypeCatalog.defaultPickerTypeIds + "ramen_restaurant",
            repository.value.value.nearbyDiscoveryTypeIds,
        )

        assertEquals(NearbyTypePickerUpdateResult.Updated, update("ramen_restaurant", add = false))
        assertEquals(NearbyTypePickerUpdateResult.Unchanged, update("ramen_restaurant", add = false))
        assertEquals(
            GoogleNearbyPlaceTypeCatalog.defaultPickerTypeIds,
            repository.value.value.nearbyDiscoveryTypeIds,
        )
    }

    @Test
    fun `picker limit is enforced without changing preferences`() = runTest {
        val full = GoogleNearbyPlaceTypeCatalog.all
            .take(GoogleNearbyPlaceTypeCatalog.MAX_PICKER_TYPES)
            .map { it.storageId }
        val repository = FakeSavedPlacesPreferencesRepository(
            SavedPlacesPreferences(nearbyDiscoveryTypeIds = full)
        )
        val update = UpdateNearbyTypePickerUseCase(repository)
        val extra = GoogleNearbyPlaceTypeCatalog.all.first { it.storageId !in full }.storageId

        assertEquals(NearbyTypePickerUpdateResult.LimitReached, update(extra, add = true))
        assertEquals(full, repository.value.value.nearbyDiscoveryTypeIds)
    }
}
