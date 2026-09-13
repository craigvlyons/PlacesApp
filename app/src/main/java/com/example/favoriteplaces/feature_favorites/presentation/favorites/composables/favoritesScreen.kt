package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.OrderType
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesEvent
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesUiEvent
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesUiState
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesViewModel
import com.example.favoriteplaces.feature_favorites.presentation.favorites.SavedPlacesFilters
import com.example.favoriteplaces.feature_favorites.presentation.favorites.SavedPlaceTypeFilter
import com.example.favoriteplaces.feature_favorites.presentation.favorites.openOpenTableSearch
import com.example.favoriteplaces.feature_favorites.presentation.favorites.openPlaceDialer
import com.example.favoriteplaces.feature_favorites.presentation.favorites.openGooglePlaceSearch
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.SwipeToDeleteContainer
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var expandedActionId by rememberSaveable { mutableStateOf<Int?>(null) }

    fun callPlace(favorite: Favorite) {
        val opened = favorite.phoneNumber?.let { openPlaceDialer(context, it) } == true
        if (!opened) scope.launch { snackbar.showSnackbar("No phone app is available.") }
    }

    fun reservePlace(favorite: Favorite) {
        val result = openOpenTableSearch(context, favorite.title)
        val message = when {
            result.opened && result.copied -> "Restaurant name copied. OpenTable search opened."
            result.opened -> "OpenTable search opened."
            result.copied -> "OpenTable is unavailable. Restaurant name copied."
            else -> "OpenTable could not be opened."
        }
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun searchPlaceOnWeb(favorite: Favorite) {
        if (!openGooglePlaceSearch(context, favorite.title, favorite.address)) {
            scope.launch { snackbar.showSnackbar("A web browser could not be opened.") }
        }
    }

    fun togglePlaceActions(favorite: Favorite) {
        val id = favorite.id ?: return
        val opening = expandedActionId != id
        expandedActionId = id.takeIf { opening }
        if (opening) viewModel.loadPlaceActionsIfNeeded(favorite)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is FavoritesUiEvent.ShowSnackbar -> snackbar.showSnackbar(event.message)
                is FavoritesUiEvent.PlaceDeleted -> {
                    if (
                        snackbar.showSnackbar("Place deleted", "Undo") ==
                        SnackbarResult.ActionPerformed
                    ) {
                        viewModel.onEvent(FavoritesEvent.RestoreFavorite(event.favorite))
                    }
                }
            }
        }
    }

    if (showFilters) {
        SavedPlacesFilterSheet(
            state = state,
            onDismiss = { showFilters = false },
            onApply = { filters, order, grouped ->
                viewModel.onEvent(FavoritesEvent.ApplyFilters(filters))
                viewModel.onEvent(FavoritesEvent.Order(order))
                if (grouped != state.isListView) {
                    viewModel.onEvent(FavoritesEvent.ToggleListOrCardView)
                }
                showFilters = false
            },
            onClear = {
                viewModel.onEvent(FavoritesEvent.ClearFilters)
                showFilters = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("saved_color_header"),
                title = { Text("Places") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.SettingsScreen.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.favorites.size == 1) "1 place" else "${state.favorites.size} places",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showFilters = true }) {
                    Text(if (state.filters.activeCount == 0) "Filter" else "Filter (${state.filters.activeCount})")
                }
            }
            if (state.filters.activeCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = state.filters.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { viewModel.onEvent(FavoritesEvent.ClearFilters) }) {
                        Text("Clear")
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.errorMessage != null -> EmptySavedState(
                    title = "Saved places unavailable",
                    message = state.errorMessage.orEmpty(),
                    actionLabel = "Retry",
                    onAction = { viewModel.onEvent(FavoritesEvent.RetryLoading) },
                )
                state.favorites.isEmpty() -> EmptySavedState(
                    title = if (state.filters.activeCount > 0) "No matching places" else "No saved places yet",
                    message = if (state.filters.activeCount > 0) {
                        "Try clearing a filter."
                    } else {
                        "Use Find to add a place you want to remember."
                    },
                )
                state.isListView -> GroupedSavedPlaces(
                    state = state,
                    navController = navController,
                    onFavoriteChanged = { id, selected ->
                        viewModel.onEvent(FavoritesEvent.LovedFavorite(id, selected))
                    },
                    onDelete = { favorite -> viewModel.onEvent(FavoritesEvent.DeleteFavorite(favorite)) },
                    expandedActionId = expandedActionId,
                    loadingActionIds = state.loadingActionIds,
                    onActionsToggle = ::togglePlaceActions,
                    onCall = ::callPlace,
                    onReserve = ::reservePlace,
                    onWeb = ::searchPlaceOnWeb,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.favorites,
                        key = { it.id ?: it.placeId ?: "${it.title}|${it.address}" },
                    ) { favorite ->
                        SwipeToDeleteContainer(
                            item = favorite,
                            onDelete = { viewModel.onEvent(FavoritesEvent.DeleteFavorite(it)) },
                        ) { item ->
                            FavoriteItem(
                                favorite = item,
                                modifier = Modifier.fillMaxWidth().testTag("favorite_card_${item.id}"),
                                onOpen = { item.id?.let { navController.navigate(Screen.PlaceDetailsScreen.route(it)) } },
                                onMapClick = {
                                    item.id?.let { navController.navigate(Screen.CityMapScreen.favoriteRoute(it)) }
                                },
                                onLovedClick = { selected ->
                                    item.id?.let { viewModel.onEvent(FavoritesEvent.LovedFavorite(it, selected)) }
                                },
                                actionsExpanded = expandedActionId == item.id,
                                actionsLoading = item.id in state.loadingActionIds,
                                onActionsToggle = { togglePlaceActions(item) },
                                onCall = { callPlace(item) },
                                onReserve = { reservePlace(item) },
                                onWeb = { searchPlaceOnWeb(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupedSavedPlaces(
    state: FavoritesUiState,
    navController: NavController,
    onFavoriteChanged: (Int, Boolean) -> Unit,
    onDelete: (Favorite) -> Unit,
    expandedActionId: Int?,
    loadingActionIds: Set<Int>,
    onActionsToggle: (Favorite) -> Unit,
    onCall: (Favorite) -> Unit,
    onReserve: (Favorite) -> Unit,
    onWeb: (Favorite) -> Unit,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.cityColorList, key = { it.city }) { city ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                city.colorVariations.forEach { group ->
                    FavoriteListView(
                        city = city.city,
                        favoritesList = group,
                        onOpen = { favorite ->
                            favorite.id?.let { navController.navigate(Screen.PlaceDetailsScreen.route(it)) }
                        },
                        onLovedClick = { favorite, selected ->
                            favorite.id?.let { id -> onFavoriteChanged(id, selected) }
                        },
                        onMapSelect = { favorite ->
                            favorite.id?.let {
                                navController.navigate(Screen.CityMapScreen.favoriteRoute(it))
                            }
                        },
                        onDelete = onDelete,
                        expandedActionId = expandedActionId,
                        loadingActionIds = loadingActionIds,
                        onActionsToggle = onActionsToggle,
                        onCall = onCall,
                        onReserve = onReserve,
                        onWeb = onWeb,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedPlacesFilterSheet(
    state: FavoritesUiState,
    onDismiss: () -> Unit,
    onApply: (SavedPlacesFilters, FavoriteOrder, Boolean) -> Unit,
    onClear: () -> Unit,
) {
    var draft by remember(state.filters) { mutableStateOf(state.filters) }
    var order by remember(state.favoriteOrder) { mutableStateOf(state.favoriteOrder) }
    var grouped by remember(state.isListView) { mutableStateOf(state.isListView) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("saved_places_filter_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Filter saved places", style = MaterialTheme.typography.headlineSmall) }
            item { SheetLabel("City") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.availableCities, key = { it }) { city ->
                        FilterChip(
                            selected = draft.city == city,
                            onClick = { draft = draft.copy(city = city.takeUnless { draft.city == city }) },
                            label = { Text(city, maxLines = 1) },
                        )
                    }
                }
            }
            item { SheetLabel("Color") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    state.availableColors.forEachIndexed { index, color ->
                        val selected = draft.color == color
                        Box(
                            Modifier
                                .size(48.dp)
                                .semantics {
                                    role = Role.RadioButton
                                    this.selected = selected
                                    contentDescription = "Color ${index + 1} of ${state.availableColors.size}"
                                }
                                .clickable {
                                    draft = draft.copy(color = color.takeUnless { selected })
                                }
                                .padding(if (selected) 5.dp else 8.dp)
                                .background(Color(color), CircleShape)
                        )
                    }
                }
            }
            item { SheetLabel("Type") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.hasPlacesWithoutType) {
                        item(key = "type_not_set") {
                            val filter = SavedPlaceTypeFilter.NotSet
                            FilterChip(
                                selected = draft.placeType == filter,
                                onClick = {
                                    draft = draft.copy(
                                        placeType = filter.takeUnless { draft.placeType == filter }
                                    )
                                },
                                label = { Text("Type not set") },
                            )
                        }
                    }
                    items(state.availablePlaceTypes, key = { "type_$it" }) { type ->
                        val filter = SavedPlaceTypeFilter.Exact(type)
                        FilterChip(
                            selected = draft.placeType == filter,
                            onClick = {
                                draft = draft.copy(
                                    placeType = filter.takeUnless { draft.placeType == filter }
                                )
                            },
                            label = { Text(type, maxLines = 1) },
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Favorites only")
                    Switch(
                        checked = draft.favoriteOnly,
                        onCheckedChange = { draft = draft.copy(favoriteOnly = it) },
                    )
                }
            }
            item { HorizontalDivider() }
            item { SheetLabel("Sort") }
            items(sortOptions()) { option ->
                Row(
                    Modifier.fillMaxWidth().clickable { order = option }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = order == option, onClick = { order = option })
                    Text(option.label(), modifier = Modifier.padding(start = 8.dp))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column { Text("Group by city"); Text("Includes group map actions", style = MaterialTheme.typography.bodySmall) }
                    Switch(
                        checked = grouped,
                        onCheckedChange = { grouped = it },
                        modifier = Modifier.testTag("group_by_city_switch"),
                    )
                }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag("saved_places_filter_actions"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear all") }
                    Button(
                        onClick = { onApply(draft, order, grouped) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Apply") }
                }
            }
        }
    }
}

@Composable private fun SheetLabel(text: String) = Text(text, style = MaterialTheme.typography.titleSmall)

@Composable
private fun EmptySavedState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) {
                Text(actionLabel)
            }
        }
    }
}

private fun SavedPlacesFilters.summary(): String = buildList {
    city?.let(::add)
    if (color != null) add("Color selected")
    if (favoriteOnly) add("Favorites")
    when (val type = placeType) {
        null -> Unit
        SavedPlaceTypeFilter.NotSet -> add("Type not set")
        is SavedPlaceTypeFilter.Exact -> add(type.value)
    }
}.joinToString(" · ")

private fun sortOptions(): List<FavoriteOrder> = listOf(
    FavoriteOrder.City(OrderType.Ascending),
    FavoriteOrder.City(OrderType.Descending),
    FavoriteOrder.Rating(OrderType.Descending),
    FavoriteOrder.IsFavorite(OrderType.Descending),
    FavoriteOrder.Color(OrderType.Ascending),
)

private fun FavoriteOrder.label(): String = when (this) {
    is FavoriteOrder.City -> if (orderType is OrderType.Ascending) "City A–Z" else "City Z–A"
    is FavoriteOrder.Rating -> "Highest rating"
    is FavoriteOrder.IsFavorite -> "Favorites first"
    is FavoriteOrder.Color -> "Color"
}
