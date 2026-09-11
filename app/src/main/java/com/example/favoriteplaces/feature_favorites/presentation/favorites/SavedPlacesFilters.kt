package com.example.favoriteplaces.feature_favorites.presentation.favorites

data class SavedPlacesFilters(
    val city: String? = null,
    val color: Int? = null,
    val favoriteOnly: Boolean = false,
    val placeType: SavedPlaceTypeFilter? = null,
) {
    val activeCount: Int
        get() = listOfNotNull(city, color, favoriteOnly.takeIf { it }, placeType).size
}

sealed interface SavedPlaceTypeFilter {
    data class Exact(val value: String) : SavedPlaceTypeFilter
    data object NotSet : SavedPlaceTypeFilter
}
