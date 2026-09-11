package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.OrderType

data class FavoritesUiState(
    val favorites: List<Favorite> = emptyList(),
    val favoriteOrder: FavoriteOrder = FavoriteOrder.City(OrderType.Descending),
    val cityColorList: List<CityGroupUiModel> = emptyList(),
    val isListView: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val filters: SavedPlacesFilters = SavedPlacesFilters(),
    val availableCities: List<String> = emptyList(),
    val availableColors: List<Int> = emptyList(),
    val availablePlaceTypes: List<String> = emptyList(),
    val hasPlacesWithoutType: Boolean = false,
    val loadingActionIds: Set<Int> = emptySet(),
)
