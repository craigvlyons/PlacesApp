package com.example.favoriteplaces.feature_favorites.di

import android.content.Context
import android.location.Geocoder
import com.example.favoriteplaces.feature_favorites.data.data_source.api.GooglePlacesApi
import com.example.favoriteplaces.feature_favorites.data.repository.PlacesRepositoryImpl
import com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase.GetPlaceDetailsUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase.GetPredictionsUseCase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlacesModule {
    @Singleton
    @Provides
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(GooglePlacesApi.BASE_URL)
            .build()
    }
    @Singleton
    @Provides
    fun providesGooglePlacesApi(retrofit: Retrofit): GooglePlacesApi{
        return retrofit.create(GooglePlacesApi::class.java)
    }

    @Singleton
    @Provides
    fun providesPlacesRepositoryImpl(placesApi: GooglePlacesApi): PlacesRepositoryImpl{
        return PlacesRepositoryImpl(
            placesApi, apiKey = "AIzaSyCIqp5O_6r5xb9tphh1vEzHtAo41y5yJ_I"
        )
    }

    @Provides
    @Singleton
    fun providesPredictionUseCase(placesRepositoryImpl: PlacesRepositoryImpl) : GetPredictionsUseCase {
        return GetPredictionsUseCase(placesRepositoryImpl)
    }

    @Provides
    @Singleton
    fun providesPlacesDetailsUseCase(placesRepositoryImpl: PlacesRepositoryImpl) : GetPlaceDetailsUseCase {
        return GetPlaceDetailsUseCase(placesRepositoryImpl)
    }

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


}
