package com.example.favoriteplaces.feature_favorites.presentation.util

sealed class PermissionAction {
    object OnPermissionGranted : PermissionAction()
    object OnPermissionDenied : PermissionAction()

}


