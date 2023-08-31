package com.example.favoriteplaces.feature_favorites.presentation.edit_favorite

import androidx.compose.ui.focus.FocusState

sealed class EditFavoriteEvent {
    data class EnteredTitle(val value: String): EditFavoriteEvent()
    data class ChangeTitleFocus(val focusState: FocusState): EditFavoriteEvent()
    data class EnteredContent(val value: String): EditFavoriteEvent()
    data class ChangeContentFocus(val focusState: FocusState): EditFavoriteEvent()
    data class ChangeColor(val color: Int) : EditFavoriteEvent()
    data class ChangeRating(val rating: Int) : EditFavoriteEvent()
    object MapSelect : EditFavoriteEvent()
    object SaveFavorite: EditFavoriteEvent()
}
