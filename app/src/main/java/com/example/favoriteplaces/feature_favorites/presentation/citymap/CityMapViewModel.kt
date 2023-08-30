package com.example.favoriteplaces.feature_favorites.presentation.citymap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.MapItems
import com.example.favoriteplaces.feature_favorites.domain.use_case.cacheusecase.MapItemsCacheUseCases
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityMapViewModel @Inject constructor(
    private val mapItemsCacheUseCases: MapItemsCacheUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _cityData = MutableStateFlow<MapItems?>(null)
    val cityData: StateFlow<MapItems?> = _cityData

    private val _startLocation = MutableStateFlow<LatLng>(LatLng(0.0, 0.0))
    val startLocation = _startLocation

    // one time events, for snackBar.
    private val _eventFlow = MutableSharedFlow<CityMapViewModel.UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            mapItemsCacheUseCases.getMapItems().collect { mapItems ->
                _cityData.value = mapItems
            }
        }
        if (_cityData.value != null){
            _startLocation.value = _cityData.value!!.mapItems[0].getPosition()
        }
    }


    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        //object SaveFavorite : UiEvent()
    }

}