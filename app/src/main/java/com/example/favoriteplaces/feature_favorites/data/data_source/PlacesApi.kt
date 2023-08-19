package com.example.favoriteplaces.feature_favorites.data.data_source

import android.util.Log
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


interface ApiRepository {
    suspend fun searchPlaces(query: String): List<Place>
}

class ApiRepositoryImpl @Inject constructor(private val placesClient: PlacesClient) : ApiRepository {
        override suspend fun searchPlaces(query: String): List<Place> {
            try {
                val request = FindAutocompletePredictionsRequest
                    .builder()
                    .setQuery(query)
                    .build()
                Log.e("resultTag", "request: $request")
                val response = placesClient.findAutocompletePredictions(request)
                Log.e("resultTag", "response: $response")
                if (response.isCanceled) {
                    throw CancellationException("Request was canceled")
                } else if (!response.isSuccessful) {
                    // Log or handle the error
                    val status = response.exception
                    Log.e("resultTagError", "Failed status: $status")
                    val errorMessage = status?.message ?: "Unknown error"
                    Log.e("resultTagError", "Failed to fetch places: $errorMessage")
                    throw Exception("Failed to fetch places: $errorMessage")
                }

                val predictions = response.result.autocompletePredictions
                val places = mutableListOf<Place>()

                for (prediction in predictions) {
                    val placeId = prediction.placeId
                    val place = fetchPlaceDetails(placeId)
                    places.add(place)
                }

                return places
            } catch (e: Exception) {
                // Log or handle the error
                Log.e("resultTag", "Error during searchPlaces: ${e.message}")
                throw e
            }
        }


    private suspend fun fetchPlaceDetails(placeId: String): Place {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.builder(placeId, fields).build()
            val response = placesClient.fetchPlace(request)

            if (response.isSuccessful) {
               return response.result.place
            } else {
                throw Exception("Failed to fetch place details")
            }
        }

}

