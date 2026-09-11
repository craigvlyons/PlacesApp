package com.example.favoriteplaces.feature_favorites.presentation.util

sealed interface PermissionAction {
    data object OnPermissionGranted : PermissionAction
    data object OnPermissionDenied : PermissionAction
}

