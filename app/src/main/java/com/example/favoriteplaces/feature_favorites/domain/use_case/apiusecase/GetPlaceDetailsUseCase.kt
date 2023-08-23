package com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase

import com.example.favoriteplaces.feature_favorites.data.models.placedetails.Result
import com.example.favoriteplaces.feature_favorites.data.repository.PlacesRepositoryImpl
import com.example.favoriteplaces.feature_favorites.data.repository.Resource
import kotlinx.coroutines.flow.Flow

class GetPlaceDetailsUseCase(
    private val placesRepositoryImpl: PlacesRepositoryImpl
) {
    suspend operator fun invoke(placeId: String) : Flow<Resource<Result>> {
        return placesRepositoryImpl.getPlaceDetails(placeId)
    }
}
