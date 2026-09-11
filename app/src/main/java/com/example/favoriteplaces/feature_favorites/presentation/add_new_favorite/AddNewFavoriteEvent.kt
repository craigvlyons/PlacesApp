package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction

sealed class AddNewFavoriteEvent {
    data class EnteredSearch(val value:String): AddNewFavoriteEvent()
    data class SelectedResult(val predictionResult: PlacePrediction): AddNewFavoriteEvent()
    data class EnteredOriginSearch(val value: String) : AddNewFavoriteEvent()
    data class SelectedOriginResult(val predictionResult: PlacePrediction) : AddNewFavoriteEvent()
    data class SelectedOriginOnMap(val latitude: Double, val longitude: Double) : AddNewFavoriteEvent()
    data object SaveFavorite : AddNewFavoriteEvent()

    object ToggleMapSelection: AddNewFavoriteEvent()
    object Search: AddNewFavoriteEvent()
    data object SearchOrigin : AddNewFavoriteEvent()
    data object UseDeviceLocation : AddNewFavoriteEvent()
}
