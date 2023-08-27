package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.presentation.util.OrderType

data class FavoritesUiState(
    val favorites: List<Favorite> = emptyList(),
    val favoriteOrder: FavoriteOrder = FavoriteOrder.City(OrderType.Descending),
    val isOrderSelectionVisible: Boolean = false,
    val isListView: Boolean = true,
    val isCardView: Boolean = false,
    val isMapSectionVisible: Boolean = false,
)