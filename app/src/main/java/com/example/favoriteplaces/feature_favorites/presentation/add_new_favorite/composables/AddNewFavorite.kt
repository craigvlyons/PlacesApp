package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteEvent
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteViewModel
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.LocationPermissionState
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.PermissionUI
import com.example.favoriteplaces.feature_favorites.presentation.util.PermissionAction
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AddNewFavorite(
    navController: NavController,
    context: Context,
    viewModel: AddNewFavoriteViewModel = hiltViewModel()
) {
    val searchString = viewModel.favoriteTitle.value
    val uiState by viewModel.uiState

    val locationPermissionState = viewModel.locationState
    val currentLocation by viewModel.currentLocation
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current

    when (locationPermissionState) {
        is LocationPermissionState.RequestPermission -> {
            // Request permission here
            // For example, you can trigger the permission request when the composable is first launched
            //viewModel.onEvent(AddNewFavoriteEvent.RequestLocationPermission)
        }
        is LocationPermissionState.LocationLoading -> {
            // Display loading state if needed
            Text("Loading location...")
        }
        is LocationPermissionState.LocationAvailable -> {
            // Location permission granted, proceed with your UI
            // You can use locationPermissionState.location to access the location
        }
        is LocationPermissionState.Error -> {
            // Handle permission denied or error state
            // For example, show a message to the user
            Text("Location permission denied")
        }
        else -> {}
    }


    // Observing and controlling the camera's state can be done with a CameraPositionState
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 11f)
    }
    var columnScrollingEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(currentLocation ){
        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLocation, 11f)
    }

    // Use a LaunchedEffect keyed on the camera moving state to enable column scrolling when the camera stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            columnScrollingEnabled = true
            Log.d("mapTag", "Map camera stopped moving - Enabling column scrolling...")
        }
    }
    // Check if location permission is requested
    val isRequestingLocationPermission = locationPermissionState is LocationPermissionState.RequestPermission

    // Request location permission when the composable is first launched
    LaunchedEffect(isRequestingLocationPermission) {
        if (isRequestingLocationPermission) {
            viewModel.onEvent(AddNewFavoriteEvent.RequestLocationPermission)
        }
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {

                is AddNewFavoriteViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }

                is AddNewFavoriteViewModel.UiEvent.SaveFavorite -> {
                    navController.navigateUp()
                }

                else -> {  }
            }
        }
    }

    Scaffold(
        scaffoldState = scaffoldState
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Show the PermissionUI if permission is requested
            if (isRequestingLocationPermission) {
                PermissionUI(
                    context = context,
                    permission = Manifest.permission.ACCESS_FINE_LOCATION,
                    permissionRationale = "We need location permission to provide accurate results.",
                    scaffoldState = scaffoldState,
                    permissionAction = { action ->
                        when (action) {
                            PermissionAction.OnPermissionGranted -> {
                                viewModel.onLocationPermissionResult(true)
                            }
                            PermissionAction.OnPermissionDenied -> {
                                viewModel.onLocationPermissionResult(false)
                            }
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TransparentHintField(
                        text = searchString.text,
                        hint = searchString.hint,
                        onValueChange = { viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch(it)) },
                        onFocusChange = { viewModel.onEvent(AddNewFavoriteEvent.ChangeSearchFocus(it)) },
                        isHintVisible = searchString.isHintVisible,
                        textStyle = androidx.compose.material.MaterialTheme.typography.h2,
                        modifier = Modifier
                            .padding(10.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        modifier = Modifier,
                        onClick = { viewModel.onEvent(AddNewFavoriteEvent.Search) },

                        ) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.titleMedium ,
                        )
                    }
                }
                // Spacer(modifier = Modifier.height(3.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    items(uiState.predictions) { prediction ->
                        PredictionCard(
                            prediction = prediction,
                            selectedId = viewModel.selectedId.value.toString(),
                            mapVisible = uiState.isMapVisible,
                            markerLocation = currentLocation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onEvent(AddNewFavoriteEvent.SelectedResult(prediction))
                                    Log.i("locationTag", "$currentLocation")
                                },
                            cameraPositionState = cameraPositionState,
                            columnScrollingEnabled = columnScrollingEnabled,
                            onMapTouched = {
                                columnScrollingEnabled = false
                                Log.d(
                                    "mapTag",
                                    "User touched map - Disabling column scrolling after user touched this Box..."
                                )
                            },
                            onMapLoaded = {
                                //cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLocation, 11f)
                            },
                            saveLocation = {
                                viewModel.onEvent(AddNewFavoriteEvent.SaveFavorite(prediction))
                            }
                        )
                        Spacer(modifier = Modifier.height(15.dp))
                    }
                }
            }
        }
    }
}
