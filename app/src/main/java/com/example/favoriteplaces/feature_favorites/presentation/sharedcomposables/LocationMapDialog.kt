package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun LocationMapDialog(
    initial: GeoCoordinates,
    contentDescription: String,
    onBackToSearch: () -> Unit,
    onUse: (GeoCoordinates) -> Unit,
) {
    val camera = rememberCameraPositionState()
    var isMapLoaded by remember { mutableStateOf(false) }
    var selectedLocation by remember(initial) { mutableStateOf<GeoCoordinates?>(null) }
    val useDarkSystemIcons = MaterialTheme.colorScheme.onPrimary != Color.White

    LaunchedEffect(initial) {
        camera.move(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(initial.latitude, initial.longitude),
                INITIAL_ZOOM,
            )
        )
    }

    Dialog(
        onDismissRequest = onBackToSearch,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, dialogView).apply {
                isAppearanceLightStatusBars = useDarkSystemIcons
                isAppearanceLightNavigationBars = useDarkSystemIcons
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("location_map_dialog"),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBackToSearch) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close map",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(
                        text = "Choose location",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Text(
                    text = "Move and zoom the map, then tap the location where you want to drop a pin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("location_picker_map"),
                    cameraPositionState = camera,
                    mapColorScheme = ComposeMapColorScheme.LIGHT,
                    contentDescription = contentDescription,
                    uiSettings = MapUiSettings(
                        compassEnabled = true,
                        mapToolbarEnabled = false,
                        myLocationButtonEnabled = false,
                        rotationGesturesEnabled = false,
                        scrollGesturesEnabled = true,
                        tiltGesturesEnabled = false,
                        zoomControlsEnabled = true,
                        zoomGesturesEnabled = true,
                    ),
                    onMapClick = { point ->
                        selectedLocation = GeoCoordinates(point.latitude, point.longitude)
                    },
                    onMapLoaded = { isMapLoaded = true },
                ) {
                    selectedLocation?.let { selected ->
                        Marker(
                            state = rememberUpdatedMarkerState(
                                position = LatLng(selected.latitude, selected.longitude)
                            ),
                            title = "Selected location",
                        )
                    }
                }

                Surface(shadowElevation = 6.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onBackToSearch,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Search instead")
                        }
                        Button(
                            onClick = {
                                selectedLocation?.let(onUse)
                            },
                            enabled = isMapLoaded && selectedLocation != null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_map_location"),
                        ) {
                            Text("Use location")
                        }
                    }
                }
            }
        }
    }
}

private const val INITIAL_ZOOM = 11f
