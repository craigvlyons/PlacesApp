package com.example.favoriteplaces.feature_favorites.presentation.util

sealed class Screen(val route:String) {
    // add all screens here.
    object FavoritesScreen: Screen("favorites_screen")
    object AddNewFavoriteScreen: Screen("Add_New_favorite_screen")
    object EditFavoriteScreen: Screen("edit_Favorite_Screen")
}