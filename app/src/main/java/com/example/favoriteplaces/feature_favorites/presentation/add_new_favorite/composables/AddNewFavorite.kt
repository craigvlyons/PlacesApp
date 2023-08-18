package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables


import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteEvent
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteViewModel
import com.example.favoriteplaces.feature_favorites.presentation.util.PlacesAutocompleteManager


@Composable
fun AddNewFavorite(
    viewModel: AddNewFavoriteViewModel = hiltViewModel()
) {
    val placesAutocompleteManager = rememberPlacesAutocompleteManager()
    val searchString = viewModel.favoriteTitle.value
    val searchHint = viewModel.favoriteTitle

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        TransparentHintField(
            text = searchString.text,
            hint = searchString.hint,
            onValueChange = { viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch(it)) },
            onFocusChange = { viewModel.onEvent(AddNewFavoriteEvent.ChangeSearchFocus(it)) },
            isHintVisible = searchString.isHintVisible,
            textStyle = androidx.compose.material.MaterialTheme.typography.body1,
            modifier = Modifier.fillMaxHeight()
        )

    }
    // Log the locationAutofill results
    for (result in viewModel.locationAutofill) {
        Log.d("mainResult", result.address)
    }
}

@Composable
private fun rememberPlacesAutocompleteManager(): PlacesAutocompleteManager {
    val context = LocalContext.current
    return remember(context) { PlacesAutocompleteManager(context) }
}


