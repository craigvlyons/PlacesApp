package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder

sealed interface FavoritesEvent {
    data class Order(val favoriteOrder: FavoriteOrder) : FavoritesEvent
    data class DeleteFavorite(val favorite: Favorite) : FavoritesEvent
    data class RestoreFavorite(val favorite: Favorite) : FavoritesEvent
    data class LovedFavorite(val id: Int, val isFavorite: Boolean) : FavoritesEvent
    data class ApplyFilters(val filters: SavedPlacesFilters) : FavoritesEvent
    data object ToggleListOrCardView : FavoritesEvent
    data object ClearFilters : FavoritesEvent
    data object RetryLoading : FavoritesEvent
}
