package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.CityWithColors
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.presentation.util.OrderType

data class FavoritesUiState(
    val favorites: List<Favorite> = emptyList(),
    val favoriteOrder: FavoriteOrder = FavoriteOrder.City(OrderType.Descending),
    val cityColorList: List<CityWithColors> = emptyList(),
    val isOrderSelectionVisible: Boolean = false,
    val isListView: Boolean = false,
    val isCardView: Boolean = true,
    val isMapSectionVisible: Boolean = false,
)
