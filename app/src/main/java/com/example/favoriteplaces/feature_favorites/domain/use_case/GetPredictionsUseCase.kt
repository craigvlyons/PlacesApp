package com.example.favoriteplaces.feature_favorites.domain.use_case

import com.example.favoriteplaces.feature_favorites.data.models.Prediction
import com.example.favoriteplaces.feature_favorites.data.repository.PlacesRepositoryImpl
import com.example.favoriteplaces.feature_favorites.data.repository.Resource
import kotlinx.coroutines.flow.Flow

class GetPredictionsUseCase(
    private val placesRepositoryImpl: PlacesRepositoryImpl
)  {
    suspend operator fun invoke(input: String) : Flow<Resource<List<Prediction>>> {
        return placesRepositoryImpl.getPredictions(input)
    }
}
