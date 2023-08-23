package com.example.favoriteplaces.feature_favorites.data.data_source.api

import com.example.favoriteplaces.feature_favorites.data.models.predicition.PlacesResponseModel
import com.example.favoriteplaces.feature_favorites.data.models.placedetails.PlaceDetailsModel
import retrofit2.http.GET
import retrofit2.http.Query


interface GooglePlacesApi {
    @GET("maps/api/place/autocomplete/json")
    suspend fun getPredictions(
        @Query("input") input: String,
        @Query("key") key: String ,
        @Query("types") types: String = "establishment",//"bar|restaurant|food|point_of_interest|establishment",
        @Query("components") components: String = "country:us",
        //@Query("locationbias") locationbias: String = "circle:radius@38.8434428,-104.8274974",
        @Query("locationrestriction") locationrestriction: String = "circle:radius@38.8434428,-104.8274974",
        //@Query("location") location: String = "38.8434428,-104.8274974",
        //@Query("radius") radius: String = "25",
    ): PlacesResponseModel

    @GET("maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,geometry",
        @Query("key") key: String
    ): PlaceDetailsModel

    companion object {
        const val BASE_URL = "https://maps.googleapis.com/"
    }
}


// colorado springs LatLng : @38.8434428,-104.8274974