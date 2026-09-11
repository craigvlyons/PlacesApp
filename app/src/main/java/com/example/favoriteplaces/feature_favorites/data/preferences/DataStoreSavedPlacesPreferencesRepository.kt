package com.example.favoriteplaces.feature_favorites.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.model.settings.DEFAULT_APP_COLOR_ARGB
import com.example.favoriteplaces.feature_favorites.domain.model.settings.DEFAULT_CARD_COLOR_ARGB
import com.example.favoriteplaces.feature_favorites.domain.model.settings.isValidArgbColor
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.logging.PrivacySafeLog
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSavedPlacesPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SavedPlacesPreferencesRepository {
    override val preferences: Flow<SavedPlacesPreferences> = dataStore.data
        .catch { exception ->
            if (exception is CancellationException) throw exception
            if (exception !is IOException) throw exception
            PrivacySafeLog.error(TAG, "Failed to read saved-screen preferences", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            SavedPlacesPreferences(
                isListView = preferences[LIST_VIEW] ?: false,
                favoriteOrder = FavoriteOrder.fromStorageValue(preferences[FAVORITE_ORDER])
                    ?: FavoriteOrder.Default,
                appColor = preferences[APP_COLOR]
                    ?.takeIf(Int::isValidArgbColor)
                    ?: DEFAULT_APP_COLOR_ARGB,
                defaultCardColor = preferences[DEFAULT_CARD_COLOR]
                    ?.takeIf(Int::isValidArgbColor)
                    ?: DEFAULT_CARD_COLOR_ARGB,
                nearbyDiscoveryTypeIds = preferences[NEARBY_DISCOVERY_TYPES]
                    ?.let(::decodeNearbyTypeIds)
                    ?: GoogleNearbyPlaceTypeCatalog.defaultPickerTypeIds,
            )
        }
        .distinctUntilChanged()

    override suspend fun setListView(isListView: Boolean) {
        dataStore.edit { preferences -> preferences[LIST_VIEW] = isListView }
    }

    override suspend fun setFavoriteOrder(favoriteOrder: FavoriteOrder) {
        dataStore.edit { preferences ->
            preferences[FAVORITE_ORDER] = favoriteOrder.storageValue()
        }
    }

    override suspend fun setAppColor(color: Int) {
        require(color.isValidArgbColor()) { "App color must be a non-transparent ARGB value." }
        dataStore.edit { preferences -> preferences[APP_COLOR] = color }
    }

    override suspend fun setDefaultCardColor(color: Int) {
        require(color.isValidArgbColor()) { "Default card color must be a non-transparent ARGB value." }
        dataStore.edit { preferences -> preferences[DEFAULT_CARD_COLOR] = color }
    }

    override suspend fun setNearbyDiscoveryTypeIds(typeIds: List<String>) {
        val trimmed = typeIds.map(String::trim)
        val normalized = GoogleNearbyPlaceTypeCatalog.normalizePickerTypeIds(trimmed)
        require(trimmed == trimmed.distinct() && normalized == trimmed) {
            "Nearby picker types must be unique, supported Google types within the picker limit."
        }
        dataStore.edit { preferences ->
            // An empty value is intentional and means the picker contains only Any type.
            preferences[NEARBY_DISCOVERY_TYPES] = normalized.joinToString(TYPE_ID_SEPARATOR)
        }
    }

    private fun decodeNearbyTypeIds(value: String): List<String> =
        GoogleNearbyPlaceTypeCatalog.normalizePickerTypeIds(
            if (value.isEmpty()) emptyList() else value.split(TYPE_ID_SEPARATOR)
        )

    companion object {
        const val DATASTORE_NAME = "saved_places"
        const val LEGACY_PREFERENCES_NAME = "app_preferences"
        val LIST_VIEW = booleanPreferencesKey("list_view")
        val FAVORITE_ORDER = stringPreferencesKey("favorite_order")
        val APP_COLOR = intPreferencesKey("app_color")
        val DEFAULT_CARD_COLOR = intPreferencesKey("default_card_color")
        val NEARBY_DISCOVERY_TYPES = stringPreferencesKey("nearby_discovery_types")
        private const val TYPE_ID_SEPARATOR = ","
        private const val TAG = "SavedPlacesPrefs"
    }
}
