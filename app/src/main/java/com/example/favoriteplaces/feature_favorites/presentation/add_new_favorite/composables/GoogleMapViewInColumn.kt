package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables

import android.util.Log
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.favoriteplaces.features.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
//import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


@Composable
fun GoogleMapViewInColumn(
    modifier: Modifier,
    currentLocation: LatLng,
    updateCameraPosition: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 11f)
    }
    val locationState = rememberMarkerState(position = cameraPositionState.position.target)

    var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }

    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = onMapLoaded
    ) {
        updateCameraPosition(currentLocation)
        // Drawing on the map is accomplished with a child-based API
        val markerClick: (Marker) -> Boolean = {
            Log.d("mapTag", "${it.title} was clicked")
            cameraPositionState.projection?.let { projection ->
                Log.d("mapTag", "The current projection is: $projection")
            }
            false
        }
        MarkerInfoWindowContent(
            state = locationState,
            title = "add title",
            onClick = markerClick,
            draggable = true,
        ) {
            Text(it.title ?: "Title", color = Color.Red)
        }
    }
}