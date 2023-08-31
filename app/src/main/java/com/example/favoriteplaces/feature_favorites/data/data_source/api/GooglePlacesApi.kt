package com.example.favoriteplaces.feature_favorites.data.data_source.api

import com.example.favoriteplaces.feature_favorites.data.models.placedetails.PlaceDetailsModel
import com.example.favoriteplaces.feature_favorites.data.models.predicition.PlacesResponseModel
import retrofit2.http.GET
import retrofit2.http.Query


interface GooglePlacesApi {
    @GET("maps/api/place/autocomplete/json")
    suspend fun getPredictions(
        @Query("input") input: String,
        @Query("key") key: String ,
        @Query("types") types: String = "establishment",
        @Query("components") components: String = "country:us",
        @Query("location") location: String,
        @Query("radius") radius: String = "75",
    ): PlacesResponseModel

    @GET("maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,geometry,formatted_address",
        @Query("key") key: String
    ): PlaceDetailsModel

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"
    }
}

