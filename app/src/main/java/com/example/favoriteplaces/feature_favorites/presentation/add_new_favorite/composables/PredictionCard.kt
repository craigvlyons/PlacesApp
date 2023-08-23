package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.data.models.predicition.Prediction
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberMarkerState


@SuppressLint("SuspiciousIndentation", "UnrememberedMutableState")
@Composable
fun PredictionCard(
    prediction: Prediction,
    selectedId: String,
    mapVisible: Boolean,
    markerLocation: LatLng,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    cameraPositionState: CameraPositionState,
    columnScrollingEnabled: Boolean,
    onMapTouched: () -> Unit,
    onMapLoaded: () -> Unit,
    saveLocation: () -> Unit,
) {
    val locationState = rememberMarkerState(position = cameraPositionState.position.target)

    LaunchedEffect(markerLocation ){
        locationState.position = markerLocation
    }

    var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }

    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    Box(
        modifier = modifier
            .shadow(6.dp, shape = RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.primary)
            .border(0.5.dp, Color.DarkGray, shape = RoundedCornerShape(cornerRadius))

    ) {
        var isMapLoaded by remember { mutableStateOf(false) }

        Canvas(modifier = Modifier.matchParentSize()) {
            val clipPath = Path().apply {
                lineTo(size.width, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            clipPath(clipPath) {
                drawRoundRect(
                    color = Color.Transparent,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Title
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = prediction.structuredFormatting.mainText,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Address
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = prediction.structuredFormatting.secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val joinedString = prediction.types.joinToString(" | ")
                Text(
                    text = joinedString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3
                )
            }

            // Show map if selected
            if (selectedId == prediction.placeId && mapVisible) {
                Text(text = "Marker Location : $markerLocation")
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {

                    // do just a map not map in column

                    GoogleMap(
                        modifier = modifier,
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = uiSettings,
                        onMapLoaded = onMapLoaded
                    ) {
                        //updateCameraPosition(markerLocation)
                        if (isMapLoaded){
                            //locationState.position = markerLocation // .animatePosition(markerLocation)
                            //locationState.marker
                        }

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
                            title = prediction.structuredFormatting.mainText,
                            onClick = markerClick,
                            draggable = true,
                        ) {
                            androidx.compose.material.Text(it.title ?: "Title", color = Color.Red)
                        }
                    }



                }
                Row(){
                    Button(onClick = { saveLocation() }) {
                        Text(text = "Save Favorite")
                    }
                }

            }
        }

    }
}
