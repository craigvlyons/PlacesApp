package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesViewModel
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.Places
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    context: Context,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val colorado = LatLng(38.8434428,-104.8274974)
    val defaultCameraPosition = CameraPosition.fromLatLngZoom(colorado, 11f)
//    val locationPermissionState = rememberMultiplePermissionsState(
//        listOf(
//            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
//        )
//    )

    // Initialize location-related components
    LaunchedEffect(Unit) {
        val fusedLocationClient ={ LocationServices.getFusedLocationProviderClient(context) }
        val placesClient ={ Places.createClient(context) }
        val geoCoder = { Geocoder(context) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AddNewFavoriteScreen.route)
                },

                backgroundColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add favorite",
                    tint = Color.White
                )
            }
        },
        scaffoldState = scaffoldState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }
            var columnScrollingEnabled by remember { mutableStateOf(true) }

            // Use a LaunchedEffect keyed on the camera moving state to enable column scrolling when the camera stops moving
            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    columnScrollingEnabled = true
                    Log.d("mapTag", "Map camera stopped moving - Enabling column scrolling...")
                }
            }

            MapInColumn(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState,
                columnScrollingEnabled = columnScrollingEnabled,
                onMapTouched = {
                    columnScrollingEnabled = false
                    Log.d(
                        "mapTag",
                        "User touched map - Disabling column scrolling after user touched this Box..."
                    )
                },
                onMapLoaded = { }
            )

        }
    }


}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapInColumn(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    columnScrollingEnabled: Boolean,
    onMapTouched: () -> Unit,
    onMapLoaded: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = androidx.compose.material.MaterialTheme.colors.background
    ) {
        var isMapLoaded by remember { mutableStateOf(false) }
        val colorado = LatLng(38.8434428,-104.8274974)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),
                    columnScrollingEnabled
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.padding(10.dp))
            for (i in 1..20) {
                androidx.compose.material.Text(
                    text = "Item $i",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .testTag("Item $i")
                )
            }
            Spacer(modifier = Modifier.padding(10.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                GoogleMapViewInColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("Map")
                        .pointerInteropFilter(
                            onTouchEvent = {
                                when (it.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        onMapTouched()
                                        false
                                    }
                                    else -> {
                                        Log.d(
                                            "mapTag",
                                            "MotionEvent ${it.action} - this never triggers."
                                        )
                                        true
                                    }
                                }
                            }
                        ),
                    cameraPositionState = cameraPositionState,
                    location = colorado,
                    onMapLoaded = {
                        isMapLoaded = true
                        onMapLoaded()
                    },
                )
                if (!isMapLoaded) {
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier
                            .fillMaxSize(),
                        visible = !isMapLoaded,
                        enter = EnterTransition.None,
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(androidx.compose.material.MaterialTheme.colors.background)
                                .wrapContentSize()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.padding(10.dp))
            for (i in 21..40) {
                androidx.compose.material.Text(
                    text = "Item $i",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .testTag("Item $i")
                )
            }
            Spacer(modifier = Modifier.padding(10.dp))
        }
    }
}

@Composable
private fun GoogleMapViewInColumn(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    location : LatLng,
    onMapLoaded: () -> Unit,
) {
    val singaporeState = rememberMarkerState(position = location)

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
        // Drawing on the map is accomplished with a child-based API
        val markerClick: (Marker) -> Boolean = {
            Log.d("mapTag", "${it.title} was clicked")
            cameraPositionState.projection?.let { projection ->
                Log.d("mapTag", "The current projection is: $projection")
            }
            false
        }
        MarkerInfoWindowContent(
            state = singaporeState,
            title = "Singapore",
            onClick = markerClick,
            draggable = true,
        ) {
            androidx.compose.material.Text(it.title ?: "Title", color = Color.Red)
        }
    }
}