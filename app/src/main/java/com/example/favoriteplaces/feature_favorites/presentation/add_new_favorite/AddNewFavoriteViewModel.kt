package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddNewFavoriteViewModel @Inject constructor(
    private val placesClient: PlacesClient, // Inject PlacesClient dependency
    private val fusedLocationClient: FusedLocationProviderClient, // Inject FusedLocationProviderClient dependency
    private val geoCoder: Geocoder // Inject Geocoder dependency
) : ViewModel() {

    var locationState by mutableStateOf<LocationState>(LocationState.NoPermission)
    val locationAutofill = mutableStateListOf<AutocompleteResult>()
    private val _selectedAutofill =
        mutableStateOf<AutocompleteResult>(AutocompleteResult(
            address = "",
            placeId = "",
            location = LatLng(0.0, 0.0),
        ))
    val selectedResult: State<AutocompleteResult> = _selectedAutofill

    private var currentLatLong by mutableStateOf(LatLng(0.0, 0.0))

    private val _favoriteTitle = mutableStateOf(
        AddFavoriteTextFieldState(
            hint = "Search for place..."
        )
    )
    val favoriteTitle: State<AddFavoriteTextFieldState> = _favoriteTitle

    //favoriteTitleHint
    private val _favoriteColor = mutableStateOf(Favorite.favoriteColors[0].toArgb())
    val favoriteColor: State<Int> = _favoriteColor

    private var job: Job? = null

    // one time events, for snackBar.
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Cancel any running coroutine job when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        viewModelScope.cancel()
    }

    fun onEvent(event: AddNewFavoriteEvent) {
        when (event) {
            is AddNewFavoriteEvent.EnteredSearch -> {
                _favoriteTitle.value = _favoriteTitle.value.copy(
                    text = event.value
                )
                //searchPlaces(_favoriteTitle.value.text)
                //searchPlaces(_favoriteTitle.value.text)
                //searchPlaces(_favoriteTitle.value.text)
                //searchPlaces(_favoriteTitle.value.text)
                //searchPlaces(_favoriteTitle.value.text)
            }

            is AddNewFavoriteEvent.ChangeSearchFocus -> {
                _favoriteTitle.value = _favoriteTitle.value.copy(
                    isHintVisible = !event.focusState.isFocused &&
                            _favoriteTitle.value.text.isBlank()
                )
            }

            is AddNewFavoriteEvent.SaveFavorite -> {
                TODO()
            }

            is AddNewFavoriteEvent.SelectedResult -> {
                viewModelScope.launch {
                    _selectedAutofill.value = _selectedAutofill.value.copy(
                        address = event.autocompleteResult.address,
                        placeId = event.autocompleteResult.placeId
                    )
                    _eventFlow.emit(
                        UiEvent.ShowSnackbar(
                            message = "Place selected:  ${_selectedAutofill.value.placeId} ${_selectedAutofill.value.address}"
                        )
                    )
                }
            }
        }
    }

    private fun searchPlaces(query: String) {
        job?.cancel()
        locationAutofill.clear()
        job = viewModelScope.launch {
            try {
                val request = FindAutocompletePredictionsRequest
                    .builder()
                    .setQuery(query)
                    .build()

                val response = placesClient.findAutocompletePredictions(request)
                val predictions = response.result.autocompletePredictions

                locationAutofill += predictions.map { prediction ->

                    AutocompleteResult(
                        address = prediction.getFullText(null).toString(),
                        placeId = prediction.placeId,
                        location = LatLng(0.0, 0.0)
                    )
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error getting address"))
            }
        }
    }

    fun getCoordinates(result: AutocompleteResult) {
        val placeFields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(result.placeId, placeFields)
        placesClient.fetchPlace(request)
            .addOnSuccessListener {
                if (it != null) {
                    currentLatLong = it.place.latLng!!
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        locationState = LocationState.LocationLoading
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                locationState =
                    if (location == null && locationState !is LocationState.LocationAvailable) {
                        LocationState.Error
                    } else {
                        currentLatLong = LatLng(location.latitude, location.longitude)
                        LocationState.LocationAvailable(
                            LatLng(
                                location.latitude,
                                location.longitude
                            )
                        )
                    }
            }
    }

    fun getAddress(latLng: LatLng) {
        viewModelScope.launch {
            val address = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            _favoriteTitle.value = _favoriteTitle.value.copy(
                text = address?.get(0)?.getAddressLine(0).toString()
            )
        }
    }


    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveFavorite : UiEvent()
    }
}
