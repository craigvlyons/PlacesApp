package com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase

import com.example.favoriteplaces.feature_favorites.data.models.predicition.Prediction
import com.example.favoriteplaces.feature_favorites.data.repository.PlacesRepositoryImpl
import com.example.favoriteplaces.feature_favorites.data.repository.Resource
import kotlinx.coroutines.flow.Flow

class GetPredictionsUseCase(
    private val placesRepositoryImpl: PlacesRepositoryImpl
)  {
    suspend operator fun invoke(input: String, location: String) : Flow<Resource<List<Prediction>>> {
        return placesRepositoryImpl.getPredictions(input, location)
    }
}
