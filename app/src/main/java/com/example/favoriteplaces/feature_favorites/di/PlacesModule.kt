package com.example.favoriteplaces.feature_favorites.di

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlacesModule {
    @Provides
    @Singleton
    fun providePlacesClient(@ApplicationContext context: Context): PlacesClient {
        return Places.createClient(context)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return FusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideGeocoder (@ApplicationContext context: Context): Geocoder {
        return Geocoder(context)
    }

}