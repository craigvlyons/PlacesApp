package com.example.favoriteplaces.feature_favorites.di

import android.content.Context
import android.location.Geocoder
import com.example.favoriteplaces.feature_favorites.data.data_source.ApiRepository
import com.example.favoriteplaces.feature_favorites.data.data_source.ApiRepositoryImpl
import com.example.favoriteplaces.feature_favorites.domain.use_case.SearchPlacesUseCase
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
        Places.initialize(context, "AIzaSyCIqp5O_6r5xb9tphh1vEzHtAo41y5yJ_I")
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

    @Provides
    @Singleton
    fun provideApiRepository(placesClient: PlacesClient): ApiRepository {
        return ApiRepositoryImpl(placesClient)
    }

    @Provides
    @Singleton
    fun provideSearchPlacesUseCase(apiRepository: ApiRepository): SearchPlacesUseCase {
        return SearchPlacesUseCase(apiRepository)
    }

}
