package com.example.favoriteplaces.di

import android.content.Context
import android.location.Geocoder
import com.example.favoriteplaces.feature_favorites.data.repository.GooglePlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.data.repository.UnavailablePlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.data.repository.GoogleNearbyPlacesRepository
import com.example.favoriteplaces.feature_favorites.data.repository.UnavailableNearbyPlacesRepository
import com.example.favoriteplaces.feature_favorites.data.location.AndroidCurrentLocationProvider
import com.example.favoriteplaces.feature_favorites.domain.repository.CurrentLocationProvider
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.NearbyPlacesRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshRepository
import com.example.favoriteplaces.feature_favorites.data.repository.GoogleSavedPlaceRefreshRepository
import com.example.favoriteplaces.feature_favorites.data.repository.UnavailableSavedPlaceRefreshRepository
import com.example.favoriteplaces.feature_favorites.data.repository.GoogleSavedPlaceActionMetadataRepository
import com.example.favoriteplaces.feature_favorites.data.repository.UnavailableSavedPlaceActionMetadataRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadataRepository
import com.example.favoriteplaces.feature_favorites.presentation.util.AddressResolver
import com.example.favoriteplaces.feature_favorites.presentation.util.AndroidAddressResolver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object PlacesApiModule {
    @Singleton
    @Provides
    fun providePlaceSearchRepository(
        @ApplicationContext context: Context,
    ): PlaceSearchRepository = if (Places.isInitialized()) {
        GooglePlaceSearchRepository(Places.createClient(context))
    } else {
        UnavailablePlaceSearchRepository
    }

    @Singleton
    @Provides
    fun provideNearbyPlacesRepository(
        @ApplicationContext context: Context,
    ): NearbyPlacesRepository = if (Places.isInitialized()) {
        GoogleNearbyPlacesRepository(Places.createClient(context))
    } else {
        UnavailableNearbyPlacesRepository
    }

    @Singleton
    @Provides
    fun provideSavedPlaceRefreshRepository(
        @ApplicationContext context: Context,
    ): SavedPlaceRefreshRepository = if (Places.isInitialized()) {
        GoogleSavedPlaceRefreshRepository(Places.createClient(context))
    } else {
        UnavailableSavedPlaceRefreshRepository
    }

    @Singleton
    @Provides
    fun provideSavedPlaceActionMetadataRepository(
        @ApplicationContext context: Context,
    ): SavedPlaceActionMetadataRepository = if (Places.isInitialized()) {
        GoogleSavedPlaceActionMetadataRepository(Places.createClient(context))
    } else {
        UnavailableSavedPlaceActionMetadataRepository
    }

    @Singleton
    @Provides
    fun provideCurrentLocationProvider(
        implementation: AndroidCurrentLocationProvider,
    ): CurrentLocationProvider = implementation

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideGeocoder (@ApplicationContext context: Context): Geocoder {
        return Geocoder(context)
    }

    @Provides
    @Singleton
    fun provideAddressResolver(geocoder: Geocoder): AddressResolver =
        AndroidAddressResolver(geocoder)


}
