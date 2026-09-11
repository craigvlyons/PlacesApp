package com.example.favoriteplaces.feature_favorites.domain.model.settings

sealed class OrderType {
    data object Ascending : OrderType()
    data object Descending : OrderType()

    fun storageValue(): String = when (this) {
        Ascending -> "Ascending"
        Descending -> "Descending"
    }

    companion object {
        fun fromStorageValue(value: String): OrderType? = when (value) {
            "Ascending" -> Ascending
            "Descending" -> Descending
            else -> null
        }
    }
}
