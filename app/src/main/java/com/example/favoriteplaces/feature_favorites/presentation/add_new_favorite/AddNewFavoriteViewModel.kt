package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import android.annotation.SuppressLint
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.data.models.predicition.Prediction
import com.example.favoriteplaces.feature_favorites.data.repository.Resource
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase.GetPlaceDetailsUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase.GetPredictionsUseCase
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

    private val _selectedId = MutableLiveData<String>("")
    var selectedId: LiveData<String> = _selectedId

    private val _currentLocation = mutableStateOf(LatLng(60.6065, -60.6066))
    val currentLocation: State<LatLng> = _currentLocation

    private val _uiState = mutableStateOf(AddNewFavoriteUiState())
    val uiState: State<AddNewFavoriteUiState> = _uiState

    private val _favoriteTitle = mutableStateOf(
        AddFavoriteTextFieldState(
            hint = "Search for place..."
        )
    )
    val favoriteTitle: State<AddFavoriteTextFieldState> = _favoriteTitle

    private val _favoriteColor = mutableStateOf(Favorite.favoriteColors[0].toArgb())
    val favoriteColor: State<Int> = _favoriteColor

    private var job: Job? = null
    private var locationJob: Job? = null

    // one time events, for snackBar.
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()


    // Cancel any running coroutine job when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        locationJob?.cancel()
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
                convertPredictionToFavorite(event.predictionResult)
                viewModelScope.launch {

                    _eventFlow.emit(
                        UiEvent.ShowSnackbar(
                            message = "Saving " +
                                    "${_uiState.value.favorite?.title}"
                        )
                    )
                }
            }

            is AddNewFavoriteEvent.Search -> {
                searchPlaces(_favoriteTitle.value.text)
            }

            is AddNewFavoriteEvent.SelectedResult -> {
                // this is a selected prediction.
                updateSelectedId(event.predictionResult.placeId)

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

    private fun updateIsMapVisible() {
        _uiState.value = _uiState.value.copy(
            isMapVisible = !_uiState.value.isMapVisible
        )
        Log.i(
            "resultDetails",
            "current location after isMapVisible function: ${_currentLocation.value}"
        )
    }

    private fun updateCurrentLocation(newLocation: LatLng) {
        //_currentLocation.value = newLocation
        _uiState.value = _uiState.value.copy(
            isMapVisible = true
        )
    }

    private fun updateSelectedId(newSelectedId: String) {
        _selectedId.value = newSelectedId
        getCoordinates(newSelectedId)
    }

    private fun convertPredictionToFavorite(prediction: Prediction) {
        val favorite = Favorite(
            id = 0,
            placeId = prediction.placeId,
            title = prediction.structuredFormatting.mainText,
            address = prediction.structuredFormatting.secondaryText,
            content = "",
            rating = 0,
            latitude = _currentLocation.value.latitude,
            longitude = _currentLocation.value.latitude
        )
        _uiState.value = _uiState.value.copy(
            favorite = favorite
        )

    }


    private fun getCoordinates(id: String) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                getPlaceDetailsUseCase(id).collect() { details ->
                    Log.i("resultDetails", "Details from useCase collect: ${details} ")
                    when (details) {
                        is Resource.Success -> {
                            val location = details.data.geometry?.location?.let {
                                LatLng(
                                    it.lat,
                                    it.lng
                                )
                            }
                            Log.i("resultDetails", "Location should print below")
                            if (location != null) {
                                _currentLocation.value = location
                                Log.i(
                                    "resultDetails",
                                    "Location from useCase collect: ${location} "
                                )
                            }
                        }

                        else -> {
                            Log.i("resultDetails", "when else block, error in getDetailsUseCase ")
                        }
                    }
                }
                isLoading = false

            } catch (e: Exception) {
                // _eventFlow.emit(UiEvent.ShowSnackbar("Error getting Details."))
                Log.i("resultDetails", "Failed to get details response: ${e.message}")
            }
            Log.i("resultDetails", "current location after function: ${_currentLocation.value}")
            updateIsMapVisible()
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
                        _currentLocation.value = LatLng(location.latitude, location.longitude)
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
