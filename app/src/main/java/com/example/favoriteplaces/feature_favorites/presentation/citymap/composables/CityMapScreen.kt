package com.example.favoriteplaces.feature_favorites.presentation.citymap.composables

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.presentation.citymap.CityMapViewModel
import com.example.favoriteplaces.features.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.flow.collectLatest


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CityMapScreen(
    navController: NavController,
    context: Context,
    viewModel: CityMapViewModel = hiltViewModel()
) {
    val cityData by viewModel.cityData.collectAsState()
    val cityLocation = viewModel.startLocation
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }

    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }
    // Observing and controlling the camera's state can be done with a CameraPositionState
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cityLocation.value, 11f)
    }
    var columnScrollingEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(cityLocation) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(cityLocation.value, 11f)
    }
    // Use a LaunchedEffect keyed on the camera moving state to enable column scrolling when the camera stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            columnScrollingEnabled = true
            Log.d("mapTag", "Map camera stopped moving - Enabling column scrolling...")
        }
    }
    val locationState = rememberMarkerState(position = cameraPositionState.position.target)

    LaunchedEffect(cityLocation) {
        locationState.position = cityLocation.value
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is CityMapViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }

                else -> {}
            }
        }
    }


    Scaffold(
        scaffoldState = scaffoldState
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (cityData != null) {
                    GoogleMap(
                        modifier = Modifier,
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = uiSettings,
                        onMapLoaded = {}
                    ) {
                        // Drawing on the map is accomplished with a child-based API
                        val markerClick: (Marker) -> Boolean = {
                            Log.d("mapTag", "${it.title} was clicked")
                            cameraPositionState.projection?.let { projection ->
                                Log.d("mapTag", "The current projection is: $projection")
                            }
                            false
                        }
                        cityData!!.mapItems.forEach { mapItem ->
                            val markerLocation =
                                rememberMarkerState(position = mapItem.getPosition())
                            MarkerInfoWindowContent(
                                state = markerLocation,
                                title = mapItem.itemTitle,
                                onClick = markerClick,
                                draggable = true,
                            ) {
                                Text(it.title ?: "Title", color = Color.Red)
                            }
                        }

                    }


                }
            }
            if (cityData == null) {
                Text(
                    modifier = Modifier.align(Alignment.TopCenter),
                    text = "No data received.",
                    style = MaterialTheme.typography.h5
                )
            }
        }
    }
}