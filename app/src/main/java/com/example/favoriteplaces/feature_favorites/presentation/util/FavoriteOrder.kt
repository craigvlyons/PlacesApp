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
    override fun toString(): String {
        return "${javaClass.simpleName}_${orderType::class.simpleName}"
    }

    companion object {
        fun fromString(value: String): FavoriteOrder {
            val parts = value.split("_")
            val orderType = when (parts[1]) {
                "Ascending" -> OrderType.Ascending
                "Descending" -> OrderType.Descending
                else -> throw IllegalArgumentException("Invalid OrderType: ${parts[1]}")
            }
            return when (parts[0]) {
                "IsFavorite" -> IsFavorite(orderType)
                "City" -> City(orderType)
                "Color" -> Color(orderType)
                "Rating" -> Rating(orderType)
                else -> throw IllegalArgumentException("Invalid FavoriteOrder: ${parts[0]}")
            }
        }
    }

}