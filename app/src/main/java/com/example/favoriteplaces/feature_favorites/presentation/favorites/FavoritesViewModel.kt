package com.example.favoriteplaces.feature_favorites.presentation.favorites

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.EditFavoriteViewModel
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.presentation.util.OrderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteUseCases: FavoriteUseCases
): ViewModel() {
    private val _state = mutableStateOf(FavoritesUiState())
    val state: State<FavoritesUiState> = _state

    private var recentlyDeletedFavorite: Favorite? = null

    // one time events, for snackBar.
    private val _eventFlow = MutableSharedFlow<EditFavoriteViewModel.UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        getFavorites(FavoriteOrder.City(OrderType.Descending))
    }
    fun onEvent(event: FavoritesEvent){
        when(event){
            is FavoritesEvent.Order -> {
                if (state.value.favoriteOrder::class == event.favoriteOrder::class &&
                    state.value.favoriteOrder.orderType == event.favoriteOrder.orderType
                    ){
                    return
                }
                getFavorites(event.favoriteOrder)
            }
            is FavoritesEvent.DeleteFavorite -> {
                viewModelScope.launch{
                    favoriteUseCases.deleteFavorite(event.favorite)
                    recentlyDeletedFavorite = event.favorite
                }
            }
            is FavoritesEvent.RestoreFavorite -> {
                viewModelScope.launch {
                    favoriteUseCases.addFavorite(recentlyDeletedFavorite ?: return@launch)
                    recentlyDeletedFavorite = null
                }
            }
            is FavoritesEvent.LovedFavorite -> {
                viewModelScope.launch {
                    try {
                        favoriteUseCases.updateIsFavorite(event.id, event.isFavorite)
                    }catch (e:Exception){
                        TODO(" add error handling")
                    }
                }
            }
            is FavoritesEvent.ToggleOrderSelection -> {
                _state.value = _state.value.copy(
                    isOrderSelectionVisible = !_state.value.isOrderSelectionVisible
                )
            }
            is FavoritesEvent.ToggleListOrCardView -> {
                _state.value = _state.value.copy(
                    isListView = !_state.value.isListView,
                    isCardView = !_state.value.isCardView
                )
                viewModelScope.launch {
                _eventFlow.emit(
                    EditFavoriteViewModel.UiEvent.ShowSnackbar(
                        message = "Toggle view: ListView = ${state.value.isListView} CardView = ${state.value.isCardView}"
                    )
                )
                }
            }
        }
    }


    private fun getFavorites(favoriteOrder: FavoriteOrder) {
        viewModelScope.launch {
            favoriteUseCases.getFavorites(favoriteOrder).collect() { favorites ->
                _state.value = _state.value.copy(
                    favorites = favorites,
                    favoriteOrder = favoriteOrder
                )
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        //object SaveFavorite : UiEvent()
    }

}