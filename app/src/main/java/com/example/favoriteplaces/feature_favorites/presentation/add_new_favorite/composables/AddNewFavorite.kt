package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteEvent
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteUiState
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteViewModel
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.LocationPermissionState
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.PermissionUI
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.LocationMapDialog
import com.example.favoriteplaces.feature_favorites.presentation.util.PermissionAction
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewFavorite(
    navController: NavController,
    viewModel: AddNewFavoriteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    var showLocation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddNewFavoriteViewModel.UiEvent.ShowSnackbar -> snackbar.showSnackbar(event.message)
                AddNewFavoriteViewModel.UiEvent.SaveFavorite -> {
                    navController.navigate(Screen.FavoritesScreen.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    }
                }
            }
        }
    }

    if (showLocation) {
        ChangeLocationSheet(
            state = state,
            onDismiss = { showLocation = false },
            onEvent = viewModel::onEvent,
            onLocationChosen = { showLocation = false },
        )
    }

    val requestingPermission = state.locationState is LocationPermissionState.RequestPermission
    if (requestingPermission) {
        PermissionUI(
            context = LocalContext.current,
            permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            permissionRationale = "Location helps rank results near you. You can still search without it.",
            snackbarHostState = snackbar,
            permissionAction = { action ->
                viewModel.onLocationPermissionResult(action == PermissionAction.OnPermissionGranted)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchField.text,
                onValueChange = { viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("find_search"),
                placeholder = { Text("Search for a restaurant or place") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.onEvent(AddNewFavoriteEvent.Search)
                        keyboard?.hide()
                    }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Current location", style = MaterialTheme.typography.labelSmall)
                    Text(
                        state.searchOriginLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { showLocation = true }) { Text("Change location") }
            }
            if (state.isLoading) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
            if (state.predictions.isNotEmpty()) {
                Text(
                    "Google Maps",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!state.isLoading && state.hasSearched && state.predictions.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.errorMessage ?: "No places matched that search.")
                    TextButton(onClick = { viewModel.onEvent(AddNewFavoriteEvent.Search) }) {
                        Text("Try again")
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.predictions, key = { it.placeId }) { prediction ->
                    PredictionCard(
                        prediction = prediction,
                        isSelected = state.selectedPlaceId == prediction.placeId,
                        mapVisible = state.isMapVisible,
                        coordinates = state.mapCoordinates,
                        thirdPartyAttributions = if (state.selectedPlaceId == prediction.placeId) {
                            state.thirdPartyAttributions
                        } else emptyList(),
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { viewModel.onEvent(AddNewFavoriteEvent.SelectedResult(prediction)) },
                        onToggleMap = { viewModel.onEvent(AddNewFavoriteEvent.ToggleMapSelection) },
                        onSave = { viewModel.onEvent(AddNewFavoriteEvent.SaveFavorite) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeLocationSheet(
    state: AddNewFavoriteUiState,
    onDismiss: () -> Unit,
    onEvent: (AddNewFavoriteEvent) -> Unit,
    onLocationChosen: () -> Unit,
) {
    var showMap by rememberSaveable { mutableStateOf(false) }
    if (showMap) {
        LocationMapDialog(
            initial = state.searchOrigin ?: GeoCoordinates(39.7392, -104.9903),
            contentDescription = "Choose a search location on the map",
            onBackToSearch = { showMap = false },
            onUse = { coordinates ->
                onEvent(
                    AddNewFavoriteEvent.SelectedOriginOnMap(
                        coordinates.latitude,
                        coordinates.longitude,
                    )
                )
                onLocationChosen()
            },
        )
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Change location", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = state.originQuery,
                onValueChange = { onEvent(AddNewFavoriteEvent.EnteredOriginSearch(it)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                placeholder = { Text("Type a city or address") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onEvent(AddNewFavoriteEvent.SearchOrigin) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search locations")
                    }
                },
            )
            if (state.isOriginLoading) CircularProgressIndicator(Modifier.padding(12.dp))
            state.originPredictions.take(4).forEach { prediction ->
                Column(
                    Modifier.fillMaxWidth().clickable {
                        onEvent(AddNewFavoriteEvent.SelectedOriginResult(prediction))
                        onLocationChosen()
                    }.padding(vertical = 10.dp)
                ) {
                    Text(prediction.primaryText, style = MaterialTheme.typography.titleSmall)
                    Text(
                        prediction.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    onEvent(AddNewFavoriteEvent.UseDeviceLocation)
                    onLocationChosen()
                }) { Text("Use device location") }
                TextButton(onClick = { showMap = true }) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Text("Choose on map", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
