package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables


import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener


@Composable
fun PlacesSearchBar(
    primaryColor: Color,
    onPlaceSelected:(place:Place) -> Unit
) {
    val context = LocalContext.current

    val autocompleteFragment = remember {
        AutocompleteSupportFragment.newInstance().apply {
            setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))
            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    onPlaceSelected(place)
                }

                override fun onError(status: Status) {
                    // Handle error
                }
            })
        }
    }

    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                setBackgroundColor(primaryColor.toArgb())
                addView(autocompleteFragment.requireView())
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}


