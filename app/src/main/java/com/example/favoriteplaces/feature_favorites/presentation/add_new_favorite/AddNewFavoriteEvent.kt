package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import androidx.compose.ui.focus.FocusState

sealed class AddNewFavoriteEvent {
    data class EnteredSearch(val value:String): AddNewFavoriteEvent()
    data class ChangeSearchFocus(val focusState: FocusState): AddNewFavoriteEvent()
    data class SelectedResult(val autocompleteResult: AutocompleteResult): AddNewFavoriteEvent()

    object SaveFavorite: AddNewFavoriteEvent()
}