package com.example.favoriteplaces.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.favoriteplaces.feature_favorites.data.preferences.DataStoreSavedPlacesPreferencesRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.savedPlacesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DataStoreSavedPlacesPreferencesRepository.DATASTORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { exception ->
        PrivacySafeLog.error("SavedPlacesPrefs", "Recovered corrupt saved-screen preferences", exception)
        emptyPreferences()
    },
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName =
                    DataStoreSavedPlacesPreferencesRepository.LEGACY_PREFERENCES_NAME,
            )
        )
    },
)

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    @Singleton
    abstract fun bindSavedPlacesPreferencesRepository(
        repository: DataStoreSavedPlacesPreferencesRepository,
    ): SavedPlacesPreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideSavedPlacesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.savedPlacesDataStore
    }
}
