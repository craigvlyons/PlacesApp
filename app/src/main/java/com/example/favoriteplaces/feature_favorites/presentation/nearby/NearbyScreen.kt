package com.example.favoriteplaces.feature_favorites.presentation.nearby

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyPlace
import com.example.favoriteplaces.feature_favorites.presentation.citymap.openGoogleDirections
import com.example.favoriteplaces.feature_favorites.presentation.favorites.SavedPlaceTypeFilter
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.PermissionUI
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.LocationMapDialog
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.DiscoveryTypeManager
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.PlaceTypeLabel
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.ThirdPartyAttributions
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.WordWheelOption
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.WordWheelPicker
import com.example.favoriteplaces.feature_favorites.presentation.util.PermissionAction
import com.example.favoriteplaces.logging.PrivacySafeLog
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    navController: NavController,
    viewModel: NearbyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showLocation by rememberSaveable { mutableStateOf(false) }
    var selectedSavedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedDiscoveryId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is NearbyViewModel.UiEvent.Message) snackbar.showSnackbar(event.value)
        }
    }
    LaunchedEffect(state.source) {
        selectedSavedId = null
        selectedDiscoveryId = null
    }
    if (state.needsLocationPermission) {
        PermissionUI(
            context = context,
            permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            permissionRationale = "Nearby uses your location to show distance. Saved places remain available if you choose another location instead.",
            snackbarHostState = snackbar,
            permissionAction = { action ->
                viewModel.onEvent(
                    NearbyEvent.LocationPermissionResult(action == PermissionAction.OnPermissionGranted)
                )
            },
        )
    }
    if (showFilters) {
        NearbyFilterSheet(
            state = state,
            onDismiss = { showFilters = false },
            onEvent = viewModel::onEvent,
            onApplied = { showFilters = false },
        )
    }
    if (showLocation) {
        NearbyLocationSheet(
            state = state,
            onDismiss = { showLocation = false },
            onEvent = viewModel::onEvent,
            onChosen = { showLocation = false },
        )
    }

    Scaffold(
        topBar = {
            NearbyHeader(
                source = state.source,
                onSourceSelected = { viewModel.onEvent(NearbyEvent.SelectSource(it)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NearbyToolbar(
                state = state,
                onToggleView = { viewModel.onEvent(NearbyEvent.ToggleView) },
                onChangeLocation = { showLocation = true },
                onFilter = { showFilters = true },
            )
            when {
                state.source == NearbySource.OurPlaces && state.savedErrorMessage != null ->
                    NearbyMessage(state.savedErrorMessage, "Retry") {
                        viewModel.onEvent(NearbyEvent.Retry)
                    }
                state.source == NearbySource.NewPlaces && state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                state.source == NearbySource.NewPlaces && state.errorMessage != null ->
                    NearbyMessage(state.errorMessage, "Retry") {
                        viewModel.onEvent(NearbyEvent.Retry)
                    }
                state.source == NearbySource.OurPlaces &&
                    state.savedPlaceCount > 0 && state.origin == null ->
                    NearbyMessage(
                        "Choose a location to see which saved places are nearby.",
                        "Choose location",
                    ) {
                        showLocation = true
                    }
                state.source == NearbySource.OurPlaces && state.savedResults.isEmpty() ->
                    if (state.savedPlaceCount == 0) {
                        NearbyMessage("No saved places yet.", "Find new places") {
                            viewModel.onEvent(NearbyEvent.SelectSource(NearbySource.NewPlaces))
                        }
                    } else if (state.savedFilters.activeCount > 0) {
                        NearbyMessage("No nearby saved places match these filters.", "Clear filters") {
                            viewModel.onEvent(NearbyEvent.ApplySavedFilters(SavedNearbyFilters()))
                        }
                    } else {
                        NearbyMessage(
                            "No saved places are within ${state.savedFilters.radiusMiles} miles.",
                            "Change distance",
                        ) {
                            showFilters = true
                        }
                    }
                state.source == NearbySource.NewPlaces && state.hasSearched && state.discoveryPlaces.isEmpty() ->
                    NearbyMessage("No new places matched this area.", "Change filters") {
                        showFilters = true
                    }
                state.source == NearbySource.OurPlaces && state.isMapView ->
                    SavedNearbyMap(
                        state = state,
                        onMarkerSelected = { selectedSavedId = it },
                    )
                state.source == NearbySource.OurPlaces ->
                    SavedNearbyList(
                        state = state,
                        onOpen = { navController.navigate(Screen.PlaceDetailsScreen.route(it)) },
                        onFavorite = { id, favorite ->
                            viewModel.onEvent(NearbyEvent.ToggleSavedFavorite(id, favorite))
                        },
                    )
                state.isMapView -> DiscoveryNearbyMap(
                    state = state,
                    onMarkerSelected = { selectedDiscoveryId = it },
                )
                else -> DiscoveryNearbyList(state, viewModel::onEvent)
            }
        }
    }

    state.savedResults.firstOrNull { it.favorite.id == selectedSavedId }?.let { selected ->
        ModalBottomSheet(onDismissRequest = { selectedSavedId = null }) {
            PlaceActionSheet(
                name = selected.favorite.title,
                type = selected.favorite.placeType,
                address = selected.favorite.address,
                distanceMiles = selected.distanceMiles,
                isFavorite = selected.favorite.isFavorite,
                primaryLabel = "View place",
                onFavorite = {
                    selected.favorite.id?.let { id ->
                        viewModel.onEvent(
                            NearbyEvent.ToggleSavedFavorite(id, selected.favorite.isFavorite)
                        )
                    }
                },
                onDirections = {
                    if (!openGoogleDirections(
                            context,
                            selected.favorite.latitude,
                            selected.favorite.longitude,
                            selected.favorite.placeId,
                        )
                    ) {
                        viewModel.showMessage("Directions are unavailable on this device.")
                    }
                },
                onPrimary = {
                    selected.favorite.id?.let { id ->
                        selectedSavedId = null
                        navController.navigate(Screen.PlaceDetailsScreen.route(id))
                    }
                },
            )
        }
    }

    state.discoveryPlaces.firstOrNull { it.placeId == selectedDiscoveryId }?.let { selected ->
        val saved = state.savedPlaces[selected.placeId]
        ModalBottomSheet(onDismissRequest = { selectedDiscoveryId = null }) {
            PlaceActionSheet(
                name = selected.displayName,
                type = selected.primaryType?.asDisplayType(),
                address = selected.formattedAddress,
                distanceMiles = selected.distanceMiles,
                isFavorite = saved?.isFavorite ?: false,
                primaryLabel = if (saved == null) "Save place" else "Saved",
                onFavorite = saved?.let {
                    { viewModel.onEvent(NearbyEvent.ToggleDiscoveryFavorite(selected.placeId)) }
                },
                onDirections = {
                    if (!openGoogleDirections(
                            context,
                            selected.coordinates.latitude,
                            selected.coordinates.longitude,
                            selected.placeId,
                        )
                    ) {
                        viewModel.showMessage("Directions are unavailable on this device.")
                    }
                },
                onPrimary = if (saved == null) {
                    { viewModel.onEvent(NearbyEvent.SavePlace(selected)) }
                } else null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyHeader(source: NearbySource, onSourceSelected: (NearbySource) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                NearbySource.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = source == option,
                        onClick = { onSourceSelected(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, NearbySource.entries.size),
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color.White,
                            activeContentColor = Color(0xFF17201C),
                            activeBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                        ),
                        label = { Text(if (option == NearbySource.OurPlaces) "Our places" else "New places") },
                        modifier = Modifier.testTag("nearby_source_${option.name}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyToolbar(
    state: NearbyUiState,
    onToggleView: () -> Unit,
    onChangeLocation: () -> Unit,
    onFilter: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(selected = !state.isMapView, onClick = { if (state.isMapView) onToggleView() }, label = { Text("List") })
        FilterChip(selected = state.isMapView, onClick = { if (!state.isMapView) onToggleView() }, label = { Text("Map") })
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            TextButton(
                onClick = onChangeLocation,
                modifier = Modifier.testTag("nearby_location"),
            ) {
                Text(
                    state.compactOriginLabel(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when (state.source) {
                NearbySource.OurPlaces -> state.savedResults.size.placeCountLabel("saved")
                NearbySource.NewPlaces -> state.discoveryPlaces.size.placeCountLabel("new")
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onFilter, modifier = Modifier.testTag("nearby_filter")) {
            val count = if (state.source == NearbySource.OurPlaces) state.savedFilters.activeCount else {
                (if (state.discoveryRadiusMiles != 10) 1 else 0) +
                    (if (state.discoveryCategory != NearbyCategory.All) 1 else 0)
            }
            Text(if (count > 0) "Filter · $count" else "Filter")
        }
    }
    Text(
        when (state.source) {
            NearbySource.OurPlaces -> when {
                state.origin == null -> "Choose a location · ${state.savedFilters.radiusMiles}-mile area"
                else -> "Within ${state.savedFilters.radiusMiles} miles · nearest first"
            }
            NearbySource.NewPlaces ->
                "${state.discoveryCategory.label} · ${state.discoveryRadiusMiles} miles · Google Maps"
        },
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.source == NearbySource.OurPlaces && state.skippedCoordinateCount > 0) {
        Text(
            "${state.skippedCoordinateCount} saved ${if (state.skippedCoordinateCount == 1) "place has" else "places have"} no map location.",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun Int.placeCountLabel(kind: String): String =
    "$this $kind ${if (this == 1) "place" else "places"}"

private fun NearbyUiState.compactOriginLabel(): String {
    val coordinates = origin ?: return "Choose location"
    val namedLocation = originLabel
        .takeUnless { it == "Device location" || it == "Selected map location" }
        ?.substringBefore(',')
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    return namedLocation ?: String.format(
        Locale.US,
        "%.3f, %.3f",
        coordinates.latitude,
        coordinates.longitude,
    )
}

@Composable
private fun SavedNearbyList(
    state: NearbyUiState,
    onOpen: (Int) -> Unit,
    onFavorite: (Int, Boolean) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.savedResults, key = { it.favorite.id ?: "${it.favorite.title}|${it.favorite.address}" }) { result ->
            val favorite = result.favorite
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nearby_saved_card_${favorite.id}")
                    .clickable { favorite.id?.let(onOpen) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag("nearby_saved_accent_layer_${favorite.id}")
                        .drawBehind {
                            drawRect(
                                Color(favorite.color),
                                size = Size(6.dp.toPx(), size.height),
                            )
                        },
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 20.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(favorite.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                PlaceTypeLabel(type = favorite.placeType)
                            }
                            favorite.id?.let { id ->
                                IconButton(
                                    modifier = Modifier.testTag("nearby_saved_heart_$id"),
                                    onClick = { onFavorite(id, favorite.isFavorite) },
                                ) {
                                    Icon(
                                        if (favorite.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (favorite.isFavorite) "Remove from Favorites" else "Add to Favorites",
                                        tint = if (favorite.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Text(favorite.address, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        favorite.content?.takeIf(String::isNotBlank)?.let {
                            Text(it, modifier = Modifier.padding(top = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        result.distanceMiles?.let {
                            Text(String.format(Locale.US, "%.1f miles", it), modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelMedium)
                        }
                        if (!result.hasMapLocation) {
                            Text(
                                "Map location unavailable",
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryNearbyList(state: NearbyUiState, onEvent: (NearbyEvent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.discoveryPlaces, key = NearbyPlace::placeId) { place ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(place.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    PlaceTypeLabel(type = place.primaryType?.asDisplayType())
                    Text(place.formattedAddress, modifier = Modifier.padding(top = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (place.thirdPartyAttributions.isNotEmpty()) ThirdPartyAttributions(place.thirdPartyAttributions)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(String.format(Locale.US, "%.1f miles", place.distanceMiles))
                        val saved = state.savedPlaces[place.placeId]
                        if (saved == null) {
                            TextButton(onClick = { onEvent(NearbyEvent.SavePlace(place)) }) { Text("Save") }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Saved", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(onClick = { onEvent(NearbyEvent.ToggleDiscoveryFavorite(place.placeId)) }) {
                                    Icon(
                                        if (saved.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (saved.isFavorite) "Remove from Favorites" else "Add to Favorites",
                                        tint = if (saved.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedNearbyMap(state: NearbyUiState, onMarkerSelected: (Int) -> Unit) {
    val camera = rememberCameraPositionState {
        val initial = state.origin ?: state.savedResults.firstOrNull(SavedNearbyResult::hasMapLocation)?.favorite?.let {
            GeoCoordinates(it.latitude, it.longitude)
        } ?: GeoCoordinates(39.7392, -104.9903)
        position = CameraPosition.fromLatLngZoom(LatLng(initial.latitude, initial.longitude), 11f)
    }
    var mapLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val visiblePoints = remember(state.origin, state.savedResults) {
        buildList {
            state.origin?.let { add(LatLng(it.latitude, it.longitude)) }
            state.savedResults.filter(SavedNearbyResult::hasMapLocation).forEach { result ->
                add(LatLng(result.favorite.latitude, result.favorite.longitude))
            }
        }
    }

    LaunchedEffect(mapLoaded, visiblePoints) {
        if (mapLoaded) fitAll(camera, visiblePoints)
    }
    Box(Modifier.fillMaxSize().testTag("nearby_saved_map")) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            mapColorScheme = ComposeMapColorScheme.LIGHT,
            contentDescription = "Map of ${state.savedResults.count(SavedNearbyResult::hasMapLocation)} saved places",
            onMapLoaded = { mapLoaded = true },
        ) {
            state.origin?.let { origin ->
                Marker(
                    state = rememberUpdatedMarkerState(LatLng(origin.latitude, origin.longitude)),
                    title = "Current search location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                )
            }
            state.savedResults.filter(SavedNearbyResult::hasMapLocation).forEach { result ->
                val favorite = result.favorite
                val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(favorite.color, it) }
                Marker(
                    state = rememberUpdatedMarkerState(LatLng(favorite.latitude, favorite.longitude)),
                    title = favorite.title,
                    snippet = favorite.placeType,
                    icon = BitmapDescriptorFactory.defaultMarker(hsv[0]),
                    onClick = {
                        favorite.id?.let(onMarkerSelected)
                        favorite.id != null
                    },
                )
            }
        }
        OutlinedButton(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            onClick = {
                scope.launch {
                    fitAll(camera = camera, points = visiblePoints)
                }
            },
        ) { Text("Fit results") }
    }
}

@Composable
private fun DiscoveryNearbyMap(state: NearbyUiState, onMarkerSelected: (String) -> Unit) {
    val camera = rememberCameraPositionState()
    val origin = state.origin
    var mapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(mapLoaded, origin, state.discoveryPlaces) {
        if (!mapLoaded || origin == null) return@LaunchedEffect
        fitAll(
            camera,
            listOf(LatLng(origin.latitude, origin.longitude)) + state.discoveryPlaces.map {
                LatLng(it.coordinates.latitude, it.coordinates.longitude)
            },
        )
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize().testTag("nearby_discovery_map"),
        cameraPositionState = camera,
        mapColorScheme = ComposeMapColorScheme.LIGHT,
        contentDescription = "Map of ${state.discoveryPlaces.size} new places",
        onMapLoaded = { mapLoaded = true },
    ) {
        origin?.let {
            Marker(
                state = rememberUpdatedMarkerState(LatLng(it.latitude, it.longitude)),
                title = "Current search location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
            )
        }
        state.discoveryPlaces.forEach { place ->
            Marker(
                state = rememberUpdatedMarkerState(LatLng(place.coordinates.latitude, place.coordinates.longitude)),
                title = place.displayName,
                snippet = place.primaryType?.asDisplayType(),
                onClick = { onMarkerSelected(place.placeId); true },
            )
        }
    }
}

private suspend fun fitAll(camera: com.google.maps.android.compose.CameraPositionState, points: List<LatLng>) {
    if (points.isEmpty()) return
    try {
        if (points.size == 1) {
            camera.animate(CameraUpdateFactory.newLatLngZoom(points.first(), 12f))
        } else {
            val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
            camera.animate(CameraUpdateFactory.newLatLngBounds(bounds, 96))
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IllegalStateException) {
        PrivacySafeLog.info(
            "NearbyMap",
            "Map bounds were unavailable; using the fallback camera " +
                "[${exception.javaClass.simpleName}]"
        )
        camera.move(CameraUpdateFactory.newLatLngZoom(points.first(), 10f))
    }
}

@Composable
private fun PlaceActionSheet(
    name: String,
    type: String?,
    address: String,
    distanceMiles: Double?,
    isFavorite: Boolean,
    primaryLabel: String,
    onFavorite: (() -> Unit)?,
    onDirections: () -> Unit,
    onPrimary: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PlaceTypeLabel(type = type, style = MaterialTheme.typography.labelLarge)
                Text(address, modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                distanceMiles?.let {
                    Text(
                        String.format(Locale.US, "%.1f miles away", it),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            onFavorite?.let {
                IconButton(onClick = it) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onDirections) {
            Icon(Icons.Default.Directions, contentDescription = null)
            Text("Directions", modifier = Modifier.padding(start = 8.dp))
        }
        if (onPrimary != null) {
            Button(modifier = Modifier.fillMaxWidth(), onClick = onPrimary) { Text(primaryLabel) }
        } else {
            Text(primaryLabel, modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NearbyMessage(message: String?, action: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message.orEmpty(), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onClick) { Text(action) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyFilterSheet(
    state: NearbyUiState,
    onDismiss: () -> Unit,
    onEvent: (NearbyEvent) -> Unit,
    onApplied: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        if (state.source == NearbySource.OurPlaces) {
            SavedNearbyFilterContent(state, onEvent, onApplied)
        } else {
            DiscoveryNearbyFilterContent(state, onEvent, onApplied)
        }
    }
}

@Composable
private fun SavedNearbyFilterContent(
    state: NearbyUiState,
    onEvent: (NearbyEvent) -> Unit,
    onApplied: () -> Unit,
) {
    var draft by remember(state.savedFilters) { mutableStateOf(state.savedFilters) }
    var radiusText by remember(state.savedFilters.radiusMiles) {
        mutableStateOf(state.savedFilters.radiusMiles.toString())
    }
    val radius = radiusText.toIntOrNull()?.coerceIn(1, MAX_SAVED_RADIUS_MILES)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
    ) {
        Text("Saved place filters", style = MaterialTheme.typography.headlineSmall)
        Text("Distance", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleSmall)
        Text(
            "Show saved places within this many miles of the selected location.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DistanceControl(radiusText, MAX_SAVED_RADIUS_MILES, onValueChanged = { radiusText = it })
        Text("Place type", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleSmall)
        FilterChip(selected = draft.placeType == null, onClick = { draft = draft.copy(placeType = null) }, label = { Text("All types") })
        if (state.hasSavedPlacesWithoutType) {
            FilterChip(
                selected = draft.placeType == SavedPlaceTypeFilter.NotSet,
                onClick = { draft = draft.copy(placeType = SavedPlaceTypeFilter.NotSet) },
                label = { Text("Type not set") },
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.availableSavedTypes) { type ->
                val filter = SavedPlaceTypeFilter.Exact(type)
                FilterChip(selected = draft.placeType == filter, onClick = { draft = draft.copy(placeType = filter) }, label = { Text(type) })
            }
        }
        Text("Color", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            item {
                TextButton(onClick = { draft = draft.copy(color = null) }) { Text("Any") }
            }
            items(state.availableSavedColors) { color ->
                Box(
                    Modifier
                        .size(38.dp)
                        .then(if (draft.color == color) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        .padding(4.dp)
                        .background(Color(color), CircleShape)
                        .clickable { draft = draft.copy(color = color) },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Favorites only", modifier = Modifier.weight(1f))
            Switch(checked = draft.favoriteOnly, onCheckedChange = { draft = draft.copy(favoriteOnly = it) })
        }
        Text("Minimum rating", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0..5).toList()) { rating ->
                FilterChip(
                    selected = draft.minimumRating == rating,
                    onClick = { draft = draft.copy(minimumRating = rating) },
                    label = { Text(if (rating == 0) "Any" else "$rating+") },
                )
            }
        }
        FilterActions(
            onClear = {
                draft = SavedNearbyFilters()
                radiusText = DEFAULT_SAVED_RADIUS_MILES.toString()
            },
            onApply = {
                radius?.let { selectedRadius ->
                    onEvent(NearbyEvent.ApplySavedFilters(draft.copy(radiusMiles = selectedRadius)))
                }
                onApplied()
            },
            applyEnabled = radius != null,
        )
    }
}

@Composable
private fun DiscoveryNearbyFilterContent(
    state: NearbyUiState,
    onEvent: (NearbyEvent) -> Unit,
    onApplied: () -> Unit,
) {
    var radiusText by remember(state.discoveryRadiusMiles) { mutableStateOf(state.discoveryRadiusMiles.toString()) }
    var category by remember(state.discoveryCategory) { mutableStateOf(state.discoveryCategory) }
    var managingTypes by rememberSaveable { mutableStateOf(false) }
    val radius = radiusText.toIntOrNull()?.coerceIn(1, 31)
    if (managingTypes) {
        DiscoveryTypeManager(
            configuredCategories = state.discoveryPickerCategories,
            onAdd = { storageId -> onEvent(NearbyEvent.AddDiscoveryCategory(storageId)) },
            onRemove = { storageId ->
                if (category.storageId == storageId) category = NearbyCategory.All
                onEvent(NearbyEvent.RemoveDiscoveryCategory(storageId))
            },
            onDone = { managingTypes = false },
        )
    } else {
        val wheelCategories = listOf(NearbyCategory.All) + state.discoveryPickerCategories
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("New place filters", style = MaterialTheme.typography.headlineSmall)
            Text("Distance", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleSmall)
            DistanceControl(radiusText, 31, onValueChanged = { radiusText = it })
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Place type", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(
                    onClick = { managingTypes = true },
                    modifier = Modifier.testTag("nearby_type_add"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add")
                }
            }
            WordWheelPicker(
                options = wheelCategories.map { WordWheelOption(it.storageId, it.label) },
                selectedKey = category.storageId.takeIf { selected ->
                    wheelCategories.any { it.storageId == selected }
                } ?: NearbyCategory.ALL_STORAGE_ID,
                testTagPrefix = "nearby_type",
                onSelected = { selectedId ->
                    category = wheelCategories.first { it.storageId == selectedId }
                },
            )
            Text(
                "Scroll or tap a label to center it.",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            FilterActions(
                onClear = { radiusText = "10"; category = NearbyCategory.All },
                onApply = {
                    radius?.let { onEvent(NearbyEvent.ApplyDiscoveryFilters(it, category)); onApplied() }
                },
                applyEnabled = radius != null,
            )
        }
    }
}

@Composable
private fun DistanceControl(value: String, max: Int, onValueChanged: (String) -> Unit) {
    val number = value.toIntOrNull()?.coerceIn(1, max)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChanged(((number ?: 10) - 1).coerceAtLeast(1).toString()) }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease distance")
        }
        OutlinedTextField(
            value = value,
            onValueChange = { input -> if (input.length <= 3 && input.all(Char::isDigit)) onValueChanged(input) },
            modifier = Modifier.weight(1f),
            suffix = { Text("miles") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = value.toIntOrNull() !in 1..max,
        )
        IconButton(onClick = { onValueChanged(((number ?: 10) + 1).coerceAtMost(max).toString()) }) {
            Icon(Icons.Default.Add, contentDescription = "Increase distance")
        }
    }
}

@Composable
private fun FilterActions(onClear: () -> Unit, onApply: () -> Unit, applyEnabled: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear") }
        Button(onClick = onApply, enabled = applyEnabled, modifier = Modifier.weight(1f)) { Text("Show places") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyLocationSheet(
    state: NearbyUiState,
    onDismiss: () -> Unit,
    onEvent: (NearbyEvent) -> Unit,
    onChosen: () -> Unit,
) {
    var showMap by rememberSaveable { mutableStateOf(false) }
    if (showMap) {
        LocationMapDialog(
            initial = state.origin ?: GeoCoordinates(39.7392, -104.9903),
            contentDescription = "Choose nearby location",
            onBackToSearch = { showMap = false },
            onUse = { coordinates ->
                onEvent(NearbyEvent.SelectMapOrigin(coordinates))
                onChosen()
            },
        )
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Change location", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = state.originQuery,
                onValueChange = { onEvent(NearbyEvent.EnterOriginQuery(it)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                placeholder = { Text("Type a city or address") },
                trailingIcon = {
                    IconButton(onClick = { onEvent(NearbyEvent.SearchOrigin) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search locations")
                    }
                },
                singleLine = true,
            )
            if (state.isOriginLoading) CircularProgressIndicator(Modifier.padding(10.dp))
            state.originPredictions.take(4).forEach { prediction ->
                Column(
                    Modifier.fillMaxWidth().clickable {
                        onEvent(NearbyEvent.SelectOriginPrediction(prediction))
                        onChosen()
                    }.padding(vertical = 10.dp),
                ) {
                    Text(prediction.primaryText, style = MaterialTheme.typography.titleSmall)
                    Text(prediction.secondaryText, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onEvent(NearbyEvent.UseDeviceLocation); onChosen() }) {
                    Text("Use device location")
                }
                TextButton(onClick = { showMap = true }) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Text("Choose on map", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

private fun String.asDisplayType(): String =
    replace('_', ' ').replaceFirstChar(Char::titlecase)
