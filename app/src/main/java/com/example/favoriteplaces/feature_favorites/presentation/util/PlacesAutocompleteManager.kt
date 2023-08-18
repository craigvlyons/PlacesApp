package com.example.favoriteplaces.feature_favorites.presentation.util



import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class PlacesAutocompleteManager(private val context: Context) {
    private val autocompleteFragment = AutocompleteSupportFragment.newInstance().apply {
        setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))
    }

    init {
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // Handle the selected place
            }

            override fun onError(status: Status) {
                // Handle error
            }
        })
    }

    @Composable
    fun PlacesSearchBar(primaryColor: Color, modifier: Modifier) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    setBackgroundColor(primaryColor.toArgb())
                    addView(autocompleteFragment.requireView())
                }
            },
            modifier = modifier
        )
    }
}
