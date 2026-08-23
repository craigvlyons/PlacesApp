package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates

sealed class LocationPermissionState{
    object NoPermission : LocationPermissionState()
    object RequestPermission : LocationPermissionState()
    object LocationLoading : LocationPermissionState()
    data class LocationAvailable(val location: GeoCoordinates) : LocationPermissionState()
    object Error : LocationPermissionState()
}
