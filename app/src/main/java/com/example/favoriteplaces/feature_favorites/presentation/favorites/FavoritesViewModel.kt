package com.example.favoriteplaces.feature_favorites.presentation.favorites

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AutocompleteResult
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.LocationState
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject


@HiltViewModel
class FavoritesViewModel @Inject constructor(): ViewModel() {
    private val _state = mutableStateOf(FavoritesUiState())
    val state: State<FavoritesUiState> = _state

    var locationState by mutableStateOf<LocationState>(LocationState.NoPermission)
    val locationAutofill = mutableStateListOf<AutocompleteResult>()

    var currentLatLong by mutableStateOf(LatLng(0.0, 0.0))
    private var job: Job? = null









}