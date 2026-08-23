package com.example.favoriteplaces.feature_favorites.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.ColorWheelDropdown
import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.DiscoveryTypeManager
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showPlaceTypes by rememberSaveable { mutableStateOf(false) }
    val configuredTypes = preferences.nearbyDiscoveryTypeIds.mapNotNull(
        GoogleNearbyPlaceTypeCatalog::find
    )

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest(snackbar::showSnackbar)
    }

    if (showPlaceTypes) {
        ModalBottomSheet(
            onDismissRequest = { showPlaceTypes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            DiscoveryTypeManager(
                configuredCategories = configuredTypes,
                onAdd = viewModel::addNearbyType,
                onRemove = viewModel::removeNearbyType,
                onDone = { showPlaceTypes = false },
                testTagPrefix = "settings_nearby_type",
                backContentDescription = "Back to settings",
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("settings_header"),
                title = { Text("Settings") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppearanceSetting(
                title = "App color",
                description = "Used for the app header and navigation.",
                color = preferences.appColor,
                testTagPrefix = "settings_app_color",
                onColorSelected = viewModel::setAppColor,
            )
            AppearanceSetting(
                title = "Default color for new places",
                description = "Applied when a new place is saved.",
                color = preferences.defaultCardColor,
                testTagPrefix = "settings_card_color",
                onColorSelected = viewModel::setDefaultCardColor,
            )
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPlaceTypes = true }
                    .testTag("settings_nearby_types"),
                headlineContent = { Text("New place type picker") },
                supportingContent = {
                    Text(
                        if (configuredTypes.isEmpty()) {
                            "Only Any type is shown in Nearby."
                        } else {
                            "${configuredTypes.size} quick types shown in Nearby."
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.Category, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            )
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.DataBackupScreen.route) }
                    .testTag("settings_data_backup"),
                headlineContent = { Text("Data & backup") },
                supportingContent = { Text("Export or import a portable copy of your places.") },
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun AppearanceSetting(
    title: String,
    description: String,
    color: Int,
    testTagPrefix: String,
    onColorSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ColorWheelDropdown(
            selectedColor = color,
            testTagPrefix = testTagPrefix,
            onColorSelected = onColorSelected,
        )
    }
}
