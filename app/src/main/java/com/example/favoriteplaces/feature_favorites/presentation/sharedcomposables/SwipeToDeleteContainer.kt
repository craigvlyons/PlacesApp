package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    item: Favorite,
    onDelete: (favorite: Favorite) -> Unit,
    content: @Composable (Favorite) -> Unit
) {
    val state = rememberSwipeToDismissBoxState()

    LaunchedEffect(state.currentValue, item) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete(item)
            // Room remains the source of truth. Resetting keeps the row visible if deletion fails;
            // a successful deletion removes the keyed composable when Room emits the new list.
            state.reset()
        }
    }

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            DeleteBackground(swipeDismissState = state)
        },
        enableDismissFromStartToEnd = false,
        content = { content(item) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBackground(
    swipeDismissState: SwipeToDismissBoxState
) {
    val visibility = swipeDismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart

    val color = if (visibility) Color.Red else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(10.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
    if (visibility) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete place",
            tint = Color.White,
        )
    }
    }
}
