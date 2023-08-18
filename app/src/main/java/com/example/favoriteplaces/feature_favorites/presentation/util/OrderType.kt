package com.example.favoriteplaces.feature_favorites.presentation.util

sealed class OrderType{
    object Ascending: OrderType()
    object Descending: OrderType()
}
