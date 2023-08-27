package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder

sealed class FavoritesEvent{
    // actions with passing values,
    data class Order(val favoriteOrder: FavoriteOrder): FavoritesEvent()
    data class DeleteFavorite(val favorite: Favorite): FavoritesEvent()
    data class LovedFavorite(val id: Int , val isFavorite: Boolean): FavoritesEvent()

    // actions
    object RestoreFavorite: FavoritesEvent()
    object ToggleOrderSelection: FavoritesEvent()
    object ToggleListOrCardView: FavoritesEvent()
}
