package com.example.favoriteplaces.feature_favorites.presentation.util

import android.content.Context


class PreferencesHelper(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveListViewMode(isListView: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_KEY_LIST_VIEW, isListView).apply()
    }

    fun getListViewMode(): Boolean {
        return sharedPreferences.getBoolean(PREF_KEY_LIST_VIEW, false)
    }

    fun saveFavoriteOrder(favoriteOrder: FavoriteOrder) {
        sharedPreferences.edit().putString(PREF_KEY_FAVORITE_ORDER, favoriteOrder.toString())
            .apply()
    }

    fun getFavoriteOrder(): FavoriteOrder {
        val orderString = sharedPreferences.getString(PREF_KEY_FAVORITE_ORDER, null)
        return orderString?.let {
            try {
                FavoriteOrder.fromString(it)
            } catch (e: IllegalArgumentException) {
                // Handle invalid preference value here
                // Default to a sensible default order if the saved value is invalid
                FavoriteOrder.City(OrderType.Descending)
            }
        } ?: FavoriteOrder.City(OrderType.Descending) // Default order if preference doesn't exist
    }

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val PREF_KEY_LIST_VIEW = "list_view"
        private const val PREF_KEY_FAVORITE_ORDER = "favorite_order"
    }

}
