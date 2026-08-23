package com.example.favoriteplaces.feature_favorites.presentation.place_details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceTypeFormatter
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.ColorWheelDropdown
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.PlaceTypeLabel
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsScreen(
    navController: NavController,
    viewModel: PlaceDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val placeColor = state.favorite?.let { Color(it.color) }
        ?: MaterialTheme.colorScheme.surface
    val placeBarContentColor = if (placeColor.luminance() > 0.5f) {
        Color(0xFF25252D)
    } else {
        Color.White
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PlaceDetailsViewModel.UiEvent.Message -> snackbar.showSnackbar(event.value)
                is PlaceDetailsViewModel.UiEvent.OpenMap ->
                    navController.navigate(Screen.CityMapScreen.favoriteRoute(event.favoriteId))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("details_color_header"),
                title = { Text("Place details") },
                // The app shell already places this child destination below the
                // status bar. Avoid applying the same inset a second time.
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = placeColor,
                    titleContentColor = placeBarContentColor,
                    navigationIconContentColor = placeBarContentColor,
                    actionIconContentColor = placeBarContentColor,
                ),
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.favorite?.let { favorite ->
                        IconButton(
                            modifier = Modifier
                                .testTag("details_favorite_heart")
                                .semantics {
                                    selected = favorite.isFavorite
                                    stateDescription = if (favorite.isFavorite) "Favorite" else "Not Favorite"
                                },
                            enabled = PlaceDetailsMutation.Favorite !in state.pendingMutations,
                            onClick = { viewModel.onEvent(PlaceDetailsEvent.ToggleFavorite) },
                        ) {
                            Icon(
                                if (favorite.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (favorite.isFavorite) {
                                    "Remove from Favorites"
                                } else {
                                    "Add to Favorites"
                                },
                                tint = if (favorite.isFavorite) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    placeBarContentColor
                                },
                            )
                        }
                        TextButton(
                            modifier = Modifier.testTag("details_name_type_edit"),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = placeBarContentColor,
                            ),
                            onClick = {
                                viewModel.onEvent(PlaceDetailsEvent.OpenNameAndTypeEditor)
                            },
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Text("Edit", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                },
            )
        },
        bottomBar = {
            state.favorite?.let { favorite ->
                val notesAreDirty = state.draftNotes != favorite.content.orEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(favorite.color))
                        .padding(horizontal = 16.dp)
                        .testTag("details_color_footer"),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        modifier = Modifier.testTag("details_save_notes"),
                        enabled = notesAreDirty && PlaceDetailsMutation.Notes !in state.pendingMutations,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        ),
                        onClick = { viewModel.onEvent(PlaceDetailsEvent.SaveNotes) },
                    ) {
                        if (PlaceDetailsMutation.Notes in state.pendingMutations) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            state.favorite != null -> PlaceDetailsContent(
                favorite = state.favorite ?: return@Scaffold,
                notesDraft = state.draftNotes,
                pending = state.pendingMutations,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(padding),
            )
            else -> DetailsUnavailable(
                message = state.errorMessage ?: "This saved place is unavailable.",
                onBack = navController::navigateUp,
                modifier = Modifier.padding(padding),
            )
        }
    }

    when (state.activeEditor) {
        PlaceDetailsEditor.NameAndType -> NameAndTypeSheet(state, viewModel::onEvent)
        null -> Unit
    }

    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(PlaceDetailsEvent.ContinueEditing) },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(PlaceDetailsEvent.ConfirmDiscard) }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(PlaceDetailsEvent.ContinueEditing) }) {
                    Text("Keep editing")
                }
            },
        )
    }


    state.googleDetailsReview?.let { review ->
        GoogleDetailsReviewDialog(
            review = review,
            isSaving = PlaceDetailsMutation.GoogleDetails in state.pendingMutations,
            onConfirm = { viewModel.onEvent(PlaceDetailsEvent.ConfirmGoogleDetails) },
            onDismiss = { viewModel.onEvent(PlaceDetailsEvent.DismissGoogleDetails) },
        )
    }
}

@Composable
private fun PlaceDetailsContent(
    favorite: Favorite,
    notesDraft: String,
    pending: Set<PlaceDetailsMutation>,
    onEvent: (PlaceDetailsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp).testTag("place_details"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Text(
                text = favorite.title,
                modifier = Modifier.fillMaxWidth().testTag("details_name"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            PlaceTypeLabel(
                type = favorite.placeType,
                modifier = Modifier.padding(top = 4.dp).testTag("details_type"),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Address", style = MaterialTheme.typography.labelLarge)
                Text(
                    favorite.address,
                    modifier = Modifier.padding(top = 4.dp).testTag("details_address"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                favorite.phoneNumber?.takeIf(String::isNotBlank)?.let { phone ->
                    Text(
                        "Phone",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        phone,
                        modifier = Modifier.padding(top = 4.dp).testTag("details_phone"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onEvent(PlaceDetailsEvent.OpenMap) }) {
                Icon(Icons.Default.Map, contentDescription = "Show place on map")
            }
        }
        Column {
            Text("Your rating", style = MaterialTheme.typography.labelLarge)
            RatingSelector(
                rating = favorite.rating ?: 0,
                enabled = PlaceDetailsMutation.Rating !in pending,
                onRatingSelected = { selected ->
                    onEvent(
                        PlaceDetailsEvent.SelectRating(
                            if (favorite.rating == selected) 0 else selected
                        )
                    )
                },
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            ColorWheelDropdown(
                selectedColor = favorite.color,
                enabled = PlaceDetailsMutation.Color !in pending,
                testTagPrefix = "details_color",
                onColorSelected = { onEvent(PlaceDetailsEvent.SelectColor(it)) },
            )
        }
        OutlinedTextField(
            value = notesDraft,
            onValueChange = { onEvent(PlaceDetailsEvent.EnterNotes(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("details_edit_notes"),
            enabled = PlaceDetailsMutation.Notes !in pending,
            placeholder = { Text("Add notes about this place") },
            shape = RoundedCornerShape(18.dp),
        )
    }
}

@Composable
private fun RatingSelector(
    rating: Int,
    enabled: Boolean,
    onRatingSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics {
            stateDescription = if (rating == 0) "Not rated" else "$rating of 5 stars"
        },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(5) { index ->
            val value = index + 1
            IconButton(
                enabled = enabled,
                modifier = Modifier.semantics {
                    selected = value <= rating
                    contentDescription = if (value == rating) {
                        "Rating $value of 5, selected. Double tap to clear"
                    } else {
                        "Set rating to $value"
                    }
                },
                onClick = { onRatingSelected(value) },
            ) {
                Icon(
                    if (value <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (value <= rating) Color(0xFFF5B400) else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameAndTypeSheet(
    state: PlaceDetailsUiState,
    onEvent: (PlaceDetailsEvent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onEvent(PlaceDetailsEvent.RequestEditorDismiss) }) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Edit name and type", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = state.draftName,
                onValueChange = { onEvent(PlaceDetailsEvent.EnterName(it)) },
                modifier = Modifier.fillMaxWidth().testTag("details_edit_name"),
                label = { Text("Name") },
                isError = state.draftName.isBlank(),
                supportingText = if (state.draftName.isBlank()) {
                    { Text("Name is required") }
                } else null,
                singleLine = true,
            )
            OutlinedTextField(
                value = state.draftType,
                onValueChange = {
                    if (it.length <= PlaceTypeFormatter.MAX_USER_LENGTH) {
                        onEvent(PlaceDetailsEvent.EnterType(it))
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("details_edit_type"),
                label = { Text("Place type (optional)") },
                singleLine = true,
            )
            OutlinedButton(
                enabled = state.favorite?.placeId?.isNotBlank() == true &&
                    PlaceDetailsMutation.GoogleDetails !in state.pendingMutations,
                onClick = { onEvent(PlaceDetailsEvent.CheckGoogleDetails) },
                modifier = Modifier.fillMaxWidth().testTag("details_check_google"),
            ) {
                if (PlaceDetailsMutation.GoogleDetails in state.pendingMutations) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("Check Google for updates", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(
                if (state.favorite?.placeId.isNullOrBlank()) {
                    "This place is not connected to a Google listing."
                } else {
                    "Reviews address, map location, phone, and Google category before anything changes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                enabled = state.draftName.isNotBlank() &&
                    PlaceDetailsMutation.NameAndType !in state.pendingMutations,
                onClick = { onEvent(PlaceDetailsEvent.SaveNameAndType) },
                modifier = Modifier.fillMaxWidth().testTag("details_save_name_type"),
            ) {
                if (PlaceDetailsMutation.NameAndType in state.pendingMutations) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun GoogleDetailsReviewDialog(
    review: GoogleDetailsReview,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Review Google updates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (review.addressChanged) {
                    ReviewChange(
                        label = "Address and map location",
                        oldValue = review.oldAddress,
                        newValue = review.details.address,
                    )
                }
                if (review.phoneChanged) {
                    ReviewChange(
                        label = "Phone",
                        oldValue = review.oldPhoneNumber ?: "Not saved",
                        newValue = review.details.phoneNumber ?: "Not available",
                    )
                }
                if (review.listingIdChanged) {
                    Text(
                        "Google provided a newer listing connection.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (review.listingTypeChanged) {
                    Text(
                        "Google provided updated restaurant-category information.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    "Your name, type, notes, color, rating, and Favorite heart will stay exactly as they are.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !isSaving, onClick = onConfirm) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Keep saved details") }
        },
    )
}

@Composable
private fun ReviewChange(label: String, oldValue: String, newValue: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            "Saved: $oldValue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Google: $newValue", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailsUnavailable(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Back") }
    }
}
