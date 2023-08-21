package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import android.annotation.SuppressLint
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.data.models.Prediction
import com.example.favoriteplaces.feature_favorites.data.models.placedetails.Result
import com.example.favoriteplaces.feature_favorites.data.repository.Resource
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.use_case.GetPlaceDetailsUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.GetPredictionsUseCase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
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
    private val placesClient: PlacesClient,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geoCoder: Geocoder,
    private val getPredictionsUseCase: GetPredictionsUseCase,
    private val getPlaceDetailsUseCase: GetPlaceDetailsUseCase
) : ViewModel() {
    var locationState by mutableStateOf<LocationState>(LocationState.NoPermission)
    var isLoading = false

    private val _uiState = mutableStateOf(AddNewFavoriteUiState())
    val uiState: State<AddNewFavoriteUiState> = _uiState


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

    // *** Events ***
    fun onEvent(event: AddNewFavoriteEvent) {
        when (event) {
            is AddNewFavoriteEvent.EnteredSearch -> {
                _favoriteTitle.value = _favoriteTitle.value.copy(
                    text = event.value
                )
            }

            is AddNewFavoriteEvent.ChangeSearchFocus -> {
                _favoriteTitle.value = _favoriteTitle.value.copy(
                    isHintVisible = !event.focusState.isFocused &&
                            _favoriteTitle.value.text.isBlank()
                )
            }

            is AddNewFavoriteEvent.ToggleMapSelection -> {
                // this should call an api function to get location data, passing the selected Prediction Id.
                TODO()
            }

            is AddNewFavoriteEvent.SaveFavorite -> {
                TODO()
            }

            is AddNewFavoriteEvent.Search -> {
                searchPlaces(_favoriteTitle.value.text)
            }

            is AddNewFavoriteEvent.SelectedResult -> {
                // this is a selected prediction.
                getCoordinates(event.predictionResult.placeId)
                Log.i("result", "prediction Id: ${event.predictionResult.placeId}")
            }
        }
    }

    private fun searchPlaces(query: String) {
        job?.cancel()
        // updateUiStatePredictions(emptyList())
        job = viewModelScope.launch {
            try {
                getPredictionsUseCase(query).collect() { response ->
                    when (response) {
                        is Resource.Success -> {
                            updateUiStatePredictions(response.data ?: emptyList())
                        }

                        else -> {
                            Log.i("tag", "when else block, error in getPredictionsUseCase")
                        }
                    }
                }
                isLoading = false

            } catch (e: Exception) {
                // _eventFlow.emit(UiEvent.ShowSnackbar("Error getting address"))
                Log.i("resultTag", "Failed to get response: ${e.message}")
            }
        }
    }

    // *** functions ***
    private fun updateUiStatePredictions(predictions: List<Prediction>) {
        _uiState.value = _uiState.value.copy(predictions = predictions)
    }

    private fun updateUiStateFavorite(favorite: Favorite) {
        _uiState.value = _uiState.value.copy(favorite = favorite)
    }

    private fun updateUiStateCurrentLocation(detailResult: Result) {
        val location =
            LatLng(detailResult.geometry.location.lat, detailResult.geometry.location.lng)
        _uiState.value = _uiState.value.copy(currentLocation = location)
    }

    private fun convertPredictionToFavorite(prediction: Prediction) {
        _uiState.value = _uiState.value.copy(
            favorite = Favorite(
                placeId = prediction.placeId.toInt(),
                title = prediction.structuredFormatting.mainText,
                address = prediction.structuredFormatting.secondaryText,
                content = null,
                rating = 0,
                location = _uiState.value.currentLocation
            )
        )
    }


    private fun getCoordinates(id: String) {
        viewModelScope.launch {
            try {
                getPlaceDetailsUseCase(id).collect() { details ->
                    when (details) {
                        is Resource.Success -> {
                            updateUiStateCurrentLocation(details.data.result)
                        }

                        else -> {
                            Log.i("tag", "when else block, error in getDetailsUseCase")
                        }
                    }
                }
                isLoading = false

            } catch (e: Exception) {
                // _eventFlow.emit(UiEvent.ShowSnackbar("Error getting Details."))
                Log.i("resultTag", "Failed to get details response: ${e.message}")
            }
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
