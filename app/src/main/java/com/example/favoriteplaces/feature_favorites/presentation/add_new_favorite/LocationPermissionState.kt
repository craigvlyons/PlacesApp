package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import com.google.android.gms.maps.model.LatLng

sealed class LocationPermissionState{
    object NoPermission : LocationPermissionState()
    object RequestPermission : LocationPermissionState()
    object LocationLoading : LocationPermissionState()
    data class LocationAvailable(val location: LatLng) : LocationPermissionState()
    object Error : LocationPermissionState()
}
