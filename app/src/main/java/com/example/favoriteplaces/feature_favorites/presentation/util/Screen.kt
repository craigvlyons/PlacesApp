package com.example.favoriteplaces.feature_favorites.presentation.util

import android.net.Uri

sealed class Screen(val route:String) {
    // add all screens here.
    object FavoritesScreen: Screen("favorites_screen")
    object AddNewFavoriteScreen: Screen("Add_New_favorite_screen")
    object NearbyScreen: Screen("nearby_screen")
    object PlaceDetailsScreen : Screen("place_details") {
        const val FAVORITE_ID = "favoriteId"
        const val pattern = "place_details/{$FAVORITE_ID}"

        fun route(favoriteId: Int): String = "place_details/$favoriteId"
    }
    object CityMapScreen: Screen("city_map_screen") {
        const val FAVORITE_ID = "favoriteId"
        const val CITY = "city"
        const val COLOR = "color"

        const val favoritePattern = "city_map_screen/favorite/{$FAVORITE_ID}"
        const val groupPattern = "city_map_screen/group/{$COLOR}?$CITY={$CITY}"

        fun favoriteRoute(favoriteId: Int): String =
            "city_map_screen/favorite/$favoriteId"

        fun groupRoute(city: String, color: Int): String =
            "city_map_screen/group/$color?$CITY=${Uri.encode(city)}"
    }
    object DataBackupScreen: Screen("data_backup_screen")
    object SettingsScreen: Screen("settings_screen")
}
