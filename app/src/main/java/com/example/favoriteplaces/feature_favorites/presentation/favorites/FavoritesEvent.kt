package com.example.favoriteplaces.feature_favorites.presentation.favorites

sealed class FavoritesEvent{
    // actions with passing values,
    //data class Order(val FavoriteOrder: FavoriteOrder): FavoritesEvent()
    //data class DeleteNote(val Favorite: Favorite): FavoritesEvent()

    // actions
    object RestoreNote: FavoritesEvent()
    object ToggleOrderSelection: FavoritesEvent()
}
