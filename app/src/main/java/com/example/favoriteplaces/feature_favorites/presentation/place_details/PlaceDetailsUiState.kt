package com.example.favoriteplaces.feature_favorites.presentation.place_details

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.GooglePlaceDetails

enum class PlaceDetailsEditor { NameAndType }

enum class PlaceDetailsMutation { NameAndType, Notes, Color, Rating, Favorite, GoogleDetails }

data class GoogleDetailsReview(
    val details: GooglePlaceDetails,
    val oldAddress: String,
    val oldPhoneNumber: String?,
    val addressChanged: Boolean,
    val phoneChanged: Boolean,
    val listingIdChanged: Boolean,
    val listingTypeChanged: Boolean,
)

data class PlaceDetailsUiState(
    val favorite: Favorite? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val activeEditor: PlaceDetailsEditor? = null,
    val draftName: String = "",
    val draftType: String = "",
    val draftNotes: String = "",
    val pendingMutations: Set<PlaceDetailsMutation> = emptySet(),
    val confirmDiscard: Boolean = false,
    val googleDetailsReview: GoogleDetailsReview? = null,
)

sealed interface PlaceDetailsEvent {
    data object OpenNameAndTypeEditor : PlaceDetailsEvent
    data class EnterName(val value: String) : PlaceDetailsEvent
    data class EnterType(val value: String) : PlaceDetailsEvent
    data class EnterNotes(val value: String) : PlaceDetailsEvent
    data class SelectColor(val value: Int) : PlaceDetailsEvent
    data class SelectRating(val value: Int) : PlaceDetailsEvent
    data object ToggleFavorite : PlaceDetailsEvent
    data object SaveNameAndType : PlaceDetailsEvent
    data object SaveNotes : PlaceDetailsEvent
    data object RequestEditorDismiss : PlaceDetailsEvent
    data object ConfirmDiscard : PlaceDetailsEvent
    data object ContinueEditing : PlaceDetailsEvent
    data object OpenMap : PlaceDetailsEvent
    data object CheckGoogleDetails : PlaceDetailsEvent
    data object ConfirmGoogleDetails : PlaceDetailsEvent
    data object DismissGoogleDetails : PlaceDetailsEvent
}
