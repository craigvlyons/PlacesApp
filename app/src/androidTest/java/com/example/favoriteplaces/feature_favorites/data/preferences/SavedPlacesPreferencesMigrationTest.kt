package com.example.favoriteplaces.feature_favorites.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.OrderType
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedPlacesPreferencesMigrationTest {
    @Test
    fun legacyPreferencesMigrateAndRemainWritable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val suffix = System.nanoTime().toString()
        val legacyName = "saved_places_legacy_test_$suffix"
        val dataStoreName = "saved_places_test_$suffix"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = context.preferencesDataStoreFile(dataStoreName)
        context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).edit()
            .putBoolean("list_view", true)
            .putString("favorite_order", "Rating_Ascending")
            .commit()

        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            migrations = listOf(SharedPreferencesMigration(context, legacyName)),
            scope = scope,
            produceFile = { file },
        )
        val repository = DataStoreSavedPlacesPreferencesRepository(store)

        try {
            assertEquals(
                com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences(
                    isListView = true,
                    favoriteOrder = FavoriteOrder.Rating(OrderType.Ascending),
                ),
                repository.preferences.first(),
            )
            repository.setFavoriteOrder(FavoriteOrder.Color(OrderType.Descending))
            repository.setListView(false)
            repository.setAppColor(-12_816_751)
            repository.setDefaultCardColor(-1_096_327)
            repository.setNearbyDiscoveryTypeIds(listOf("restaurant", "ramen_restaurant"))
            assertEquals(
                FavoriteOrder.Color(OrderType.Descending),
                repository.preferences.first().favoriteOrder,
            )
            assertEquals(false, repository.preferences.first().isListView)
            assertEquals(-12_816_751, repository.preferences.first().appColor)
            assertEquals(-1_096_327, repository.preferences.first().defaultCardColor)
            assertEquals(
                listOf("restaurant", "ramen_restaurant"),
                repository.preferences.first().nearbyDiscoveryTypeIds,
            )
            repository.setNearbyDiscoveryTypeIds(emptyList())
            assertEquals(emptyList<String>(), repository.preferences.first().nearbyDiscoveryTypeIds)
        } finally {
            scope.cancel()
            context.getSharedPreferences(legacyName, Context.MODE_PRIVATE).edit().clear().commit()
            File(file.absolutePath).delete()
        }
    }
}
