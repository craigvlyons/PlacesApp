package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import androidx.compose.ui.focus.FocusState
import com.example.favoriteplaces.feature_favorites.data.models.predicition.Prediction

sealed class AddNewFavoriteEvent {
    data class EnteredSearch(val value:String): AddNewFavoriteEvent()
    data class ChangeSearchFocus(val focusState: FocusState): AddNewFavoriteEvent()
    data class SelectedResult(val predictionResult: Prediction): AddNewFavoriteEvent()
    data class SaveFavorite(val predictionResult: Prediction): AddNewFavoriteEvent()

    object ToggleMapSelection: AddNewFavoriteEvent()
    object Search: AddNewFavoriteEvent()
}