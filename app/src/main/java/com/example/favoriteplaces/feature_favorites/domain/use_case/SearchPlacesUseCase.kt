package com.example.favoriteplaces.feature_favorites.domain.use_case

import com.example.favoriteplaces.feature_favorites.data.data_source.ApiRepository
import com.google.android.libraries.places.api.model.Place
import javax.inject.Inject


class SearchPlacesUseCase @Inject constructor(private val apiRepository: ApiRepository) {

    suspend operator fun invoke(query: String): List<Place> {
        return apiRepository.searchPlaces(query)
    }
}