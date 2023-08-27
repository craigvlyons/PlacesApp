package com.example.favoriteplaces.feature_favorites.presentation.edit_favorite

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditFavoriteViewModel @Inject constructor(
    private val favoriteUseCases: FavoriteUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val favoriteState = mutableStateOf(EditFavoriteUiState(favorite = null))
    private val _favoriteTitle = mutableStateOf(
        FavoriteTextFieldState(hint = "Enter a title...")
    )
    val favoriteTitle: State<FavoriteTextFieldState> = _favoriteTitle

    private val _favoriteContent = mutableStateOf(
        FavoriteTextFieldState(hint = "what did you like about this place...")
    )
    val favoriteContent: State<FavoriteTextFieldState> = _favoriteContent

    private val _favoriteColor = mutableStateOf(Favorite.favoriteColors.random().toArgb())
    val favoriteColor: State<Int> = _favoriteColor

    private val _favoriteId = mutableStateOf(0)
    val favoriteId: State<Int> = _favoriteId

    private val _favoriteRating = mutableStateOf(0)
    val favoriteRating: State<Int> = _favoriteRating

    private var _address = mutableStateOf(emptyList<String>())
    val address: State<List<String>> = _address

    // one time events, for snackBar.
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // get parameters with savableStateHandel
        savedStateHandle.get<Int>("favoriteId")?.let { favoriteId ->
            if (favoriteId != -1) {
                viewModelScope.launch {
                    try {
                        favoriteUseCases.getFavorite(favoriteId)?.also { newFavorite ->
                            favoriteState.value = favoriteState.value.copy(
                                favorite = Favorite(
                                    id = newFavorite.id,
                                    placeId = newFavorite.placeId,
                                    title = newFavorite.title,
                                    address = newFavorite.address,
                                    content = newFavorite.content,
                                    rating = newFavorite.rating,
                                    isFavorite = newFavorite.isFavorite,
                                    color = newFavorite.color,
                                    city = newFavorite.city,
                                    latitude = newFavorite.latitude,
                                    longitude = newFavorite.longitude
                                )

                            )
                            _favoriteId.value = newFavorite.id!!
                            _favoriteTitle.value = _favoriteTitle.value.copy(
                                text = newFavorite.title,
                                isHintVisible = false
                            )
                            _favoriteContent.value = _favoriteContent.value.copy(
                                text = newFavorite.content ?: "",
                                isHintVisible = newFavorite.content.isNullOrBlank()
                            )

                            _favoriteColor.value = newFavorite.color
                            _favoriteRating.value = newFavorite.rating!!
                            _address.value = newFavorite.address.split(",").toMutableList()
                        }
                    } catch (e: Exception) {
                        Log.i("resultFavorite", "failure retrieving favorite by id: ${e.message}")
                        _eventFlow.emit(
                            UiEvent.ShowSnackbar(
                                message = e.message ?: "Couldn't get favorite by Id."
                            )
                        )
                    }

                }
            }
        }
    }

    fun onEvent(event: EditFavoriteEvent) {
        when (event) {
            is EditFavoriteEvent.EnteredTitle -> {
                _favoriteTitle.value = _favoriteTitle.value.copy(
                    text = event.value
                )
            }

            is EditFavoriteEvent.ChangeTitleFocus -> {
                _favoriteTitle.value = _favoriteTitle.value.copy(
                    isHintVisible = !event.focusState.isFocused &&
                            _favoriteTitle.value.text.isBlank()
                )
            }

            is EditFavoriteEvent.EnteredContent -> {
                _favoriteContent.value = _favoriteContent.value.copy(
                    text = event.value
                )
            }

            is EditFavoriteEvent.ChangeContentFocus -> {
                _favoriteContent.value = _favoriteContent.value.copy(
                    isHintVisible = !event.focusState.isFocused &&
                            _favoriteContent.value.text.isBlank()
                )
            }

            is EditFavoriteEvent.ChangeColor -> {
                _favoriteColor.value = event.color
            }

            is EditFavoriteEvent.ChangeRating -> {
                _favoriteRating.value = event.rating
            }

            is EditFavoriteEvent.SaveFavorite -> {saveFavorite()}
        }
    }

    private fun saveFavorite(){
        val saveFavorite = Favorite(
            id = favoriteState.value.favorite?.id,
            placeId = favoriteState.value.favorite?.placeId,
            title = _favoriteTitle.value.text,
            address = favoriteState.value.favorite!!.address,
            content = _favoriteContent.value.text,
            rating = _favoriteRating.value,
            isFavorite = favoriteState.value.favorite!!.isFavorite,
            color = _favoriteColor.value,
            city = favoriteState.value.favorite!!.city,
            latitude = favoriteState.value.favorite!!.latitude,
            longitude = favoriteState.value.favorite!!.longitude
        )
        viewModelScope.launch {
            try {
            favoriteUseCases.addFavorite(saveFavorite)
                _eventFlow.emit(UiEvent.SaveFavorite)
            }catch (e:Exception){
                _eventFlow.emit(
                    UiEvent.ShowSnackbar(
                        message = e.message ?: "Couldn't save Favorite."
                    )
                )
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveFavorite : UiEvent()
    }

}