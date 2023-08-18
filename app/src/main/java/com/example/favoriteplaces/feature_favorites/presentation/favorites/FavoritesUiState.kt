package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.presentation.util.OrderType

data class FavoritesUiState(
    val favorites: List<Favorite> = emptyList(),
    val favoriteOrder: FavoriteOrder = FavoriteOrder.Title(OrderType.Descending),
    val isOrderSelectionVisible: Boolean = false,
    val isMapSectionVisible: Boolean = false,
)