package com.example.favoriteplaces.feature_favorites.domain.model.settings

import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog

data class SavedPlacesPreferences(
    val isListView: Boolean = false,
    val favoriteOrder: FavoriteOrder = FavoriteOrder.Default,
    val appColor: Int = DEFAULT_APP_COLOR_ARGB,
    val defaultCardColor: Int = DEFAULT_CARD_COLOR_ARGB,
    val nearbyDiscoveryTypeIds: List<String> = GoogleNearbyPlaceTypeCatalog.defaultPickerTypeIds,
)

const val DEFAULT_APP_COLOR_ARGB: Int = -8_266_006
const val DEFAULT_CARD_COLOR_ARGB: Int = -8_266_006

fun Int.isValidArgbColor(): Boolean = (this ushr 24) != 0
