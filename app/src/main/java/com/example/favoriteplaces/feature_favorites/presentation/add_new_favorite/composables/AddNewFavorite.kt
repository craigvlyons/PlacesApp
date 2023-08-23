package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables


import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteEvent
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AddNewFavorite(
    viewModel: AddNewFavoriteViewModel = hiltViewModel()
) {
    val searchString = viewModel.favoriteTitle.value
    val uiState by viewModel.uiState
    val currentLocation by viewModel.currentLocation

    val scaffoldState = rememberScaffoldState()

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
                    //navController.navigateUp()
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
                .background(MaterialTheme.colorScheme.primary)
        ) {

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
                        onValueChange = { newValue ->
                            viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch(newValue))
                        },
                        onFocusChange = { viewModel.onEvent(AddNewFavoriteEvent.ChangeSearchFocus(it)) },
                        isHintVisible = searchString.isHintVisible,
                        textStyle = androidx.compose.material.MaterialTheme.typography.body1,
                        modifier = Modifier
                            .padding(10.dp),
                    )
                }
                // Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {

                    Button(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, Color.DarkGray, shape = RoundedCornerShape(size = 20.dp)),
                        onClick = { viewModel.onEvent(AddNewFavoriteEvent.Search) },

                        ) {
                        Text(
                            text = "Search"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

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
