package com.example.favoriteplaces.feature_favorites.presentation.util

sealed class FavoriteOrder(val orderType: OrderType) {
    class Title(orderType: OrderType): FavoriteOrder(orderType)
}