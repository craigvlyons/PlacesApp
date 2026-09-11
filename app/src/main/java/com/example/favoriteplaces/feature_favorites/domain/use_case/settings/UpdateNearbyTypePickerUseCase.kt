package com.example.favoriteplaces.feature_favorites.domain.use_case.settings

import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class NearbyTypePickerUpdateResult {
    Updated,
    Unchanged,
    Unsupported,
    LimitReached,
}

@Singleton
class UpdateNearbyTypePickerUseCase @Inject constructor(
    private val preferencesRepository: SavedPlacesPreferencesRepository,
) {
    suspend operator fun invoke(
        storageId: String,
        add: Boolean,
    ): NearbyTypePickerUpdateResult = mutex.withLock {
        val category = GoogleNearbyPlaceTypeCatalog.find(storageId)
            ?: return@withLock NearbyTypePickerUpdateResult.Unsupported
        val current = GoogleNearbyPlaceTypeCatalog.normalizePickerTypeIds(
            preferencesRepository.preferences.first().nearbyDiscoveryTypeIds
        )
        val updated = if (add) {
            if (storageId in current) return@withLock NearbyTypePickerUpdateResult.Unchanged
            if (current.size >= GoogleNearbyPlaceTypeCatalog.MAX_PICKER_TYPES) {
                return@withLock NearbyTypePickerUpdateResult.LimitReached
            }
            current + category.storageId
        } else {
            if (storageId !in current) return@withLock NearbyTypePickerUpdateResult.Unchanged
            current - category.storageId
        }
        preferencesRepository.setNearbyDiscoveryTypeIds(updated)
        NearbyTypePickerUpdateResult.Updated
    }

    private val mutex = Mutex()
}
