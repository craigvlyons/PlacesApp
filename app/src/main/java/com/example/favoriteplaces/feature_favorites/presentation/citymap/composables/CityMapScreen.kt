package com.example.favoriteplaces.feature_favorites.presentation.citymap.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Directions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.favoriteplaces.feature_favorites.presentation.citymap.CityMapViewModel
import com.example.favoriteplaces.feature_favorites.presentation.citymap.openGoogleDirections
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.PlaceTypeLabel
import com.example.favoriteplaces.logging.PrivacySafeLog
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.graphics.Color
import androidx.navigation.NavController
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CityMapScreen(
    navController: NavController,
    viewModel: CityMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf<Int?>(null) }
    val singlePlace = state.places.singleOrNull()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is CityMapViewModel.UiEvent.Message) snackbar.showSnackbar(event.value)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "Saved places map" }) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    singlePlace?.let { place ->
                        IconButton(
                            modifier = Modifier.testTag("map_directions"),
                            onClick = {
                                if (!openGoogleDirections(
                                        context = context,
                                        latitude = place.latitude,
                                        longitude = place.longitude,
                                        placeId = place.placeId,
                                    )
                                ) {
                                    viewModel.showMessage("Directions are unavailable on this device.")
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default.Directions,
                                contentDescription = "Directions to ${place.title}",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.places.isNotEmpty() -> SavedPlacesMap(
                    title = state.title,
                    places = state.places.map { place ->
                        MapMarker(
                            id = place.id,
                            position = LatLng(place.latitude, place.longitude),
                            title = place.title,
                            snippet = place.address,
                            color = place.color,
                            placeType = place.placeType,
                            isFavorite = place.isFavorite,
                        )
                    },
                    coordinateWarning = state.coordinateWarning,
                    onMarkerSelected = { selectedId = it },
                )
                else -> Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    text = state.message ?: "No saved places to map.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    val selected = state.places.firstOrNull { it.id == selectedId }
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { selectedId = null }) {
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(selected.title, style = MaterialTheme.typography.titleLarge)
                        PlaceTypeLabel(
                            type = selected.placeType,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    IconButton(
                        onClick = {
                            selected.id?.let { viewModel.updateFavoriteHeart(it, !selected.isFavorite) }
                        },
                    ) {
                        Icon(
                            if (selected.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (selected.isFavorite) {
                                "Remove from Favorites"
                            } else {
                                "Add to Favorites"
                            },
                            tint = if (selected.isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                selected.id?.let { id ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            selectedId = null
                            navController.navigate(Screen.PlaceDetailsScreen.route(id))
                        },
                    ) { Text("View place") }
                }
            }
        }
    }
}

private data class MapMarker(
    val id: Int?,
    val position: LatLng,
    val title: String,
    val snippet: String,
    val color: Int,
    val placeType: String?,
    val isFavorite: Boolean,
)

@Composable
private fun SavedPlacesMap(
    title: String,
    places: List<MapMarker>,
    coordinateWarning: String?,
    onMarkerSelected: (Int) -> Unit,
) {
    val firstPosition = places.first().position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(firstPosition, SINGLE_MARKER_ZOOM)
    }
    val uiSettings = remember { MapUiSettings(compassEnabled = false) }
    val properties = remember { MapProperties(mapType = MapType.NORMAL) }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mapLoaded, places) {
        if (!mapLoaded) return@LaunchedEffect
        try {
            if (places.size == 1) {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(firstPosition, SINGLE_MARKER_ZOOM)
                )
            } else {
                val bounds = LatLngBounds.builder().apply {
                    places.forEach { include(it.position) }
                }.build()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, BOUNDS_PADDING_PX)
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IllegalStateException) {
            PrivacySafeLog.info(
                "SavedPlacesMap",
                "Map bounds were unavailable; using the fallback camera " +
                    "[${exception.javaClass.simpleName}]"
            )
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(firstPosition, GROUP_FALLBACK_ZOOM)
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .testTag("saved_places_map_${places.size}")
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            contentDescription = "$title map with ${places.size} saved places",
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
            onMapLoaded = { mapLoaded = true },
        ) {
            places.forEach { place ->
                val hsv = FloatArray(3).also { Color.colorToHSV(place.color, it) }
                Marker(
                    state = rememberUpdatedMarkerState(position = place.position),
                    title = place.title,
                    snippet = place.snippet,
                    draggable = false,
                    icon = BitmapDescriptorFactory.defaultMarker(hsv[0]),
                    onClick = {
                        place.id?.let(onMarkerSelected)
                        place.id != null
                    },
                )
            }
        }
        if (coordinateWarning != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = coordinateWarning,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private const val SINGLE_MARKER_ZOOM = 15f
private const val GROUP_FALLBACK_ZOOM = 10f
private const val BOUNDS_PADDING_PX = 96
