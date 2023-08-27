package com.example.favoriteplaces.feature_favorites.presentation.util

sealed class FavoriteOrder(val orderType: OrderType) {
    class IsFavorite(orderType: OrderType): FavoriteOrder(orderType)
    class City(orderType: OrderType): FavoriteOrder(orderType)
    class Color(orderType: OrderType): FavoriteOrder(orderType)
    class Rating(orderType: OrderType): FavoriteOrder(orderType)

    fun copy(orderType: OrderType): FavoriteOrder {
        return when(this) {
            is IsFavorite -> IsFavorite(orderType)
            is City -> City(orderType)
            is Color -> Color(orderType)
            is Rating -> Rating(orderType)
        }
    }
}