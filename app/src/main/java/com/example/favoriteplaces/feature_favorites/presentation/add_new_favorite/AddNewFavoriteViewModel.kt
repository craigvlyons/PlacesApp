package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import android.annotation.SuppressLint
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.data.models.predicition.Prediction
import com.example.favoriteplaces.feature_favorites.data.repository.Resource
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase.GetPlaceDetailsUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.apiusecase.GetPredictionsUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
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
    private val getPlaceDetailsUseCase: GetPlaceDetailsUseCase,
    private val favoriteUseCases: FavoriteUseCases
) : ViewModel() {
    var locationState by mutableStateOf<LocationPermissionState>(LocationPermissionState.NoPermission)
    var isLoading = false

    private val _selectedId = MutableLiveData<String>("")
    var selectedId: LiveData<String> = _selectedId

    private val _currentLocation = mutableStateOf(LatLng(0.0, -0.0))
    val currentLocation: State<LatLng> = _currentLocation

    private val _formattedAddress = mutableStateOf("")

    private val _uiState = mutableStateOf(AddNewFavoriteUiState())
    val uiState: State<AddNewFavoriteUiState> = _uiState

    private val _favoriteTitle = mutableStateOf(
        AddFavoriteTextFieldState(
            hint = "Search for place..."
        )
    )
    val favoriteTitle: State<AddFavoriteTextFieldState> = _favoriteTitle
    private var job: Job? = null
    private var locationJob: Job? = null

    // one time events, for snackBar.
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        getCurrentLocation()
    }

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
                updateIsMapVisible()
            }

            is AddNewFavoriteEvent.SaveFavorite -> {
                convertPredictionToFavorite(event.predictionResult)
                viewModelScope.launch {
                    try {
                        _uiState.value.favorite?.let { favoriteUseCases.addFavorite(it) }
                        _eventFlow.emit(UiEvent.SaveFavorite)
                    } catch (e: Exception) {
                        _eventFlow.emit(
                            UiEvent.ShowSnackbar(
                                message = "Error saving: ${e.message}"
                            )
                        )
                    }
                }
            }

            is AddNewFavoriteEvent.Search -> {
                if (_favoriteTitle.value.text.isNotBlank()) {
                    searchPlaces(_favoriteTitle.value.text)
                } else {
                    viewModelScope.launch {
                        _eventFlow.emit(
                            UiEvent.ShowSnackbar(
                                message = "Search can not be blank."
                            )
                        )
                    }
                }
            }

            is AddNewFavoriteEvent.SelectedResult -> {
                // this is a selected prediction.
                updateSelectedId(event.predictionResult.placeId)
                //Log.i("result", "prediction Id: ${event.predictionResult.placeId}")
            }

            is AddNewFavoriteEvent.RequestLocationPermission -> {
                requestLocationPermission()
            }
        }
    }

    private fun searchPlaces(query: String) {
        val location: String =
            LatLng(_currentLocation.value.latitude, _currentLocation.value.longitude).toString()
        job?.cancel()
        job = viewModelScope.launch {
            try {
                getPredictionsUseCase(query, location).collect { response ->
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
                //_eventFlow.emit(UiEvent.ShowSnackbar("Error getting address"))
                Log.e("resultTag", "Failed to get response: ${e.message}")
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
    }


    private fun updateSelectedId(newSelectedId: String) {
        _selectedId.value = newSelectedId
        getCoordinates(newSelectedId)
    }

    private fun convertPredictionToFavorite(prediction: Prediction) {
        val addressList = _formattedAddress.value.split(",")
        val city = addressList[1].trim()
        val favorite = Favorite(
            id = null,
            placeId = prediction.placeId,
            title = prediction.structuredFormatting.mainText,
            address = _formattedAddress.value,
            content = "",
            rating = 0,
            city = city,
            latitude = _currentLocation.value.latitude,
            longitude = _currentLocation.value.longitude
        )
        _uiState.value = _uiState.value.copy(
            favorite = favorite
        )
        //Log.i("resultFavorite", "favorite: ${_uiState.value.favorite}")
    }


    private fun getCoordinates(id: String) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                getPlaceDetailsUseCase(id).collect() { details ->
                    //Log.i("resultDetails", "Details from useCase collect: ${details} ")
                    when (details) {
                        is Resource.Success -> {
                            val location = details.data.geometry?.location?.let {
                                LatLng(it.lat, it.lng)
                            }
                            val address = details.data.formattedAddress
                            if (address != null) {
                                _formattedAddress.value = address
                            }

                            if (location != null) {
                                _currentLocation.value = location
                            }
                        }

                        else -> {
                            Log.i("resultDetails", "when else block, error in getDetailsUseCase ")
                        }
                    }
                }
                isLoading = false

            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error getting Details: ${e.message}."))
                Log.e("resultDetails", "Failed to get details response: ${e.message}")
            }
            updateIsMapVisible()
        }
    }

    fun requestLocationPermission() {
        locationState = LocationPermissionState.RequestPermission
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            // Permission granted, start location updates
            getCurrentLocation()
        } else {
            // Handle denied permission
            locationState = LocationPermissionState.Error
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        if (locationState is LocationPermissionState.NoPermission) {
            requestLocationPermission()
            return
        }
        locationState = LocationPermissionState.LocationLoading
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    _currentLocation.value = LatLng(location.latitude, location.longitude)
                } else {
                    Log.e("LocationTag", "Location is null")
                }
                locationState = LocationPermissionState.LocationAvailable(
                    LatLng(location.latitude, location.longitude)
                )
            }
            .addOnFailureListener { e ->
                Log.e("LocationTag", "Failed to get location: ${e.message}")
                locationState = LocationPermissionState.Error
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
