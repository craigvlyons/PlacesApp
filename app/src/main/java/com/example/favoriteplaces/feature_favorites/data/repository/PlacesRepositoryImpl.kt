package com.example.favoriteplaces.feature_favorites.data.repository

import android.util.Log
import com.example.favoriteplaces.feature_favorites.data.data_source.api.GooglePlacesApi
import com.example.favoriteplaces.feature_favorites.data.models.Prediction
import com.example.favoriteplaces.feature_favorites.data.models.placedetails.PlaceDetailsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PlacesRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val apiKey: String
) {

    suspend fun getPredictions(input: String): Flow<Resource<List<Prediction>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getPredictions(input = input, apiKey)
            val prediction = response.predictions
            Log.i("results", "predictions: $prediction")
            emit(Resource.Success(prediction))
        } catch (e: Exception) {
            Log.d("resultRepository", "Exception: $e")
            emit(Resource.Error("Failed prediction: ${e.message}", data = null))
        }
    }

    suspend fun getPlaceDetails(placeId: String): Flow<Resource<PlaceDetailsModel>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getPlaceDetails(placeId, key= apiKey)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            Log.d("resultRepository", "Exception: $e")
            emit(Resource.Error("Failed to fetch place details: ${e.message}", data = null))
        }
    }
}


sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Loading(val data: Nothing? = null) : Resource<Nothing>()
    data class Error(val message: String, val data: Nothing? = null) : Resource<Nothing>()
}
