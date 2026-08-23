package com.example.favoriteplaces.feature_favorites.domain.model.settings

sealed class FavoriteOrder(open val orderType: OrderType) {
    data class IsFavorite(override val orderType: OrderType) : FavoriteOrder(orderType)
    data class City(override val orderType: OrderType) : FavoriteOrder(orderType)
    data class Color(override val orderType: OrderType) : FavoriteOrder(orderType)
    data class Rating(override val orderType: OrderType) : FavoriteOrder(orderType)

    fun withOrderType(orderType: OrderType): FavoriteOrder = when (this) {
        is IsFavorite -> IsFavorite(orderType)
        is City -> City(orderType)
        is Color -> Color(orderType)
        is Rating -> Rating(orderType)
    }

    fun storageValue(): String = "${fieldValue()}_${orderType.storageValue()}"

    private fun fieldValue(): String = when (this) {
        is IsFavorite -> "IsFavorite"
        is City -> "City"
        is Color -> "Color"
        is Rating -> "Rating"
    }

    companion object {
        val Default: FavoriteOrder = City(OrderType.Descending)

        fun fromStorageValue(value: String?): FavoriteOrder? {
            val parts = value?.split('_') ?: return null
            if (parts.size != 2) return null
            val orderType = OrderType.fromStorageValue(parts[1]) ?: return null
            return when (parts[0]) {
                "IsFavorite" -> IsFavorite(orderType)
                "City" -> City(orderType)
                "Color" -> Color(orderType)
                "Rating" -> Rating(orderType)
                else -> null
            }
        }
    }
}
