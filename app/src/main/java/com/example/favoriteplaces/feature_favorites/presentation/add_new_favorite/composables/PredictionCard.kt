package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.ThirdPartyAttributions
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun PredictionCard(
    prediction: PlacePrediction,
    isSelected: Boolean,
    mapVisible: Boolean,
    coordinates: GeoCoordinates,
    thirdPartyAttributions: List<String>,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onToggleMap: () -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick = onSelect,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                prediction.primaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                prediction.secondaryText,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSelected) {
                if (mapVisible) {
                    ConfirmationMap(prediction.primaryText, coordinates)
                }
                if (thirdPartyAttributions.isNotEmpty()) {
                    ThirdPartyAttributions(thirdPartyAttributions)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onToggleMap) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = if (mapVisible) "Hide location map" else "Show location map",
                        )
                    }
                    TextButton(onClick = onSave) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationMap(title: String, coordinates: GeoCoordinates) {
    val latLng = LatLng(coordinates.latitude, coordinates.longitude)
    val camera = rememberCameraPositionState()
    val marker = rememberUpdatedMarkerState(latLng)
    LaunchedEffect(latLng) { camera.move(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) }
    Box(Modifier.fillMaxWidth().height(190.dp).padding(top = 12.dp)) {
        GoogleMap(
            modifier = Modifier.fillMaxWidth(),
            cameraPositionState = camera,
            uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) },
            mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
            contentDescription = "Map confirming $title",
        ) {
            Marker(state = marker, title = title)
        }
    }
}
