package com.example.favoriteplaces.feature_favorites.presentation.place_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceTypeFormatter
import com.example.favoriteplaces.feature_favorites.domain.model.settings.isValidArgbColor
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteDetailsRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.distanceMilesTo
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.example.favoriteplaces.logging.PrivacySafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlaceDetailsViewModel @Inject constructor(
    private val detailsRepository: FavoriteDetailsRepository,
    private val favoriteUseCases: FavoriteUseCases,
    private val refreshRepository: SavedPlaceRefreshRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val favoriteId = savedStateHandle.get<Int>(Screen.PlaceDetailsScreen.FAVORITE_ID)
    private val mutationJobs = mutableMapOf<PlaceDetailsMutation, Job>()

    private val _state = MutableStateFlow(
        PlaceDetailsUiState(
            activeEditor = savedStateHandle.get<String>(ACTIVE_EDITOR)
                ?.let { value -> PlaceDetailsEditor.entries.firstOrNull { it.name == value } },
            draftName = savedStateHandle[DRAFT_NAME] ?: "",
            draftType = savedStateHandle[DRAFT_TYPE] ?: "",
            draftNotes = savedStateHandle[DRAFT_NOTES] ?: "",
        )
    )
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        if (favoriteId == null || favoriteId < 0) {
            _state.update {
                it.copy(isLoading = false, errorMessage = "This saved place could not be opened.")
            }
        } else {
            observeFavorite(favoriteId)
        }
    }

    fun onEvent(event: PlaceDetailsEvent) {
        when (event) {
            PlaceDetailsEvent.OpenNameAndTypeEditor -> openNameAndTypeEditor()
            is PlaceDetailsEvent.EnterName -> updateDraft(DRAFT_NAME, event.value) {
                copy(draftName = event.value)
            }
            is PlaceDetailsEvent.EnterType -> updateDraft(DRAFT_TYPE, event.value) {
                copy(draftType = event.value)
            }
            is PlaceDetailsEvent.EnterNotes -> updateDraft(DRAFT_NOTES, event.value) {
                copy(draftNotes = event.value)
            }
            is PlaceDetailsEvent.SelectColor -> updateColor(event.value)
            is PlaceDetailsEvent.SelectRating -> updateRating(event.value)
            PlaceDetailsEvent.ToggleFavorite -> toggleFavorite()
            PlaceDetailsEvent.SaveNameAndType -> saveNameAndType()
            PlaceDetailsEvent.SaveNotes -> saveNotes()
            PlaceDetailsEvent.RequestEditorDismiss -> requestEditorDismiss()
            PlaceDetailsEvent.ConfirmDiscard -> closeEditor(clearDraft = true)
            PlaceDetailsEvent.ContinueEditing -> _state.update { it.copy(confirmDiscard = false) }
            PlaceDetailsEvent.OpenMap -> openMap()
            PlaceDetailsEvent.CheckGoogleDetails -> checkGoogleDetails()
            PlaceDetailsEvent.ConfirmGoogleDetails -> confirmGoogleDetails()
            PlaceDetailsEvent.DismissGoogleDetails ->
                _state.update { it.copy(googleDetailsReview = null) }
        }
    }

    private fun observeFavorite(id: Int) {
        viewModelScope.launch {
            detailsRepository.observeFavoriteById(id)
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    PrivacySafeLog.error(TAG, "Failed to observe place details", exception)
                    _state.update {
                        it.copy(isLoading = false, errorMessage = "This saved place could not be loaded.")
                    }
                    _events.emit(UiEvent.Message("This saved place could not be loaded."))
                }
                .collect { favorite ->
                    _state.update { current ->
                        if (favorite == null) {
                            current.copy(
                                favorite = null,
                                isLoading = false,
                                errorMessage = "This saved place is no longer available.",
                            )
                        } else {
                            val shouldSyncNotes = !savedStateHandle.contains(DRAFT_NOTES) ||
                                current.draftNotes == current.favorite?.content.orEmpty()
                            val notes = if (shouldSyncNotes) {
                                favorite.content.orEmpty()
                            } else {
                                current.draftNotes
                            }
                            savedStateHandle[DRAFT_NOTES] = notes
                            current.copy(
                                favorite = favorite,
                                draftNotes = notes,
                                isLoading = false,
                                errorMessage = null,
                            )
                        }
                    }
                }
        }
    }

    private fun openNameAndTypeEditor() {
        val favorite = _state.value.favorite ?: return
        savedStateHandle[ACTIVE_EDITOR] = PlaceDetailsEditor.NameAndType.name
        savedStateHandle[DRAFT_NAME] = favorite.title
        savedStateHandle[DRAFT_TYPE] = favorite.placeType.orEmpty()
        _state.update {
            it.copy(
                activeEditor = PlaceDetailsEditor.NameAndType,
                draftName = favorite.title,
                draftType = favorite.placeType.orEmpty(),
                confirmDiscard = false,
            )
        }
    }

    private fun updateDraft(
        key: String,
        value: String,
        update: PlaceDetailsUiState.() -> PlaceDetailsUiState,
    ) {
        savedStateHandle[key] = value
        _state.update { it.update() }
    }

    private fun requestEditorDismiss() {
        _state.update { current ->
            if (current.hasDirtyEditor()) current.copy(confirmDiscard = true)
            else current.copy(activeEditor = null, confirmDiscard = false)
        }
        if (!_state.value.hasDirtyEditor()) clearEditorState()
    }

    private fun PlaceDetailsUiState.hasDirtyEditor(): Boolean {
        val favorite = favorite ?: return false
        return when (activeEditor) {
            PlaceDetailsEditor.NameAndType ->
                draftName != favorite.title || draftType != favorite.placeType.orEmpty()
            null -> false
        }
    }

    private fun closeEditor(clearDraft: Boolean) {
        if (clearDraft) clearEditorState()
        _state.update {
            it.copy(activeEditor = null, confirmDiscard = false)
        }
    }

    private fun clearEditorState() {
        savedStateHandle.remove<String>(ACTIVE_EDITOR)
        savedStateHandle.remove<String>(DRAFT_NAME)
        savedStateHandle.remove<String>(DRAFT_TYPE)
    }

    private fun saveNameAndType() {
        val id = favoriteId ?: return
        val name = _state.value.draftName.trim()
        if (name.isEmpty()) {
            viewModelScope.launch { _events.emit(UiEvent.Message("Place name cannot be empty.")) }
            return
        }
        launchMutation(PlaceDetailsMutation.NameAndType) {
            detailsRepository.updateNameAndType(
                id = id,
                title = name,
                placeType = PlaceTypeFormatter.normalizeUserValue(_state.value.draftType),
            )
            closeEditor(clearDraft = true)
        }
    }

    private fun saveNotes() {
        val id = favoriteId ?: return
        val notes = _state.value.draftNotes
        launchMutation(PlaceDetailsMutation.Notes) {
            detailsRepository.updateNotes(id, notes)
        }
    }

    private fun updateColor(color: Int) {
        val current = _state.value.favorite ?: return
        if (!color.isValidArgbColor()) {
            viewModelScope.launch { _events.emit(UiEvent.Message("That color could not be used.")) }
            return
        }
        if (current.color == color) return
        launchMutation(PlaceDetailsMutation.Color) {
            detailsRepository.updateColor(favoriteId ?: return@launchMutation, color)
        }
    }

    private fun updateRating(rating: Int) {
        val current = _state.value.favorite ?: return
        if (rating !in 0..5) {
            viewModelScope.launch { _events.emit(UiEvent.Message("Choose a rating from 0 to 5.")) }
            return
        }
        val normalized = rating.takeIf { it in 1..5 }
        if (current.rating == normalized) return
        launchMutation(PlaceDetailsMutation.Rating) {
            detailsRepository.updateRating(favoriteId ?: return@launchMutation, normalized)
        }
    }

    private fun toggleFavorite() {
        val current = _state.value.favorite ?: return
        launchMutation(PlaceDetailsMutation.Favorite) {
            favoriteUseCases.updateIsFavorite(favoriteId ?: return@launchMutation, !current.isFavorite)
        }
    }

    private fun openMap() {
        val id = _state.value.favorite?.id ?: return
        viewModelScope.launch { _events.emit(UiEvent.OpenMap(id)) }
    }

    private fun checkGoogleDetails() {
        val favorite = _state.value.favorite ?: return
        val placeId = favorite.placeId?.trim()?.takeIf(String::isNotEmpty)
        if (placeId == null) {
            viewModelScope.launch {
                _events.emit(UiEvent.Message("This place is not connected to a Google listing."))
            }
            return
        }
        launchMutation(PlaceDetailsMutation.GoogleDetails) {
            when (val result = refreshRepository.refresh(placeId)) {
                is SavedPlaceRefreshResult.Failure -> {
                    _events.emit(UiEvent.Message(result.reason.toRefreshMessage()))
                }
                is SavedPlaceRefreshResult.Success -> {
                    val current = _state.value.favorite ?: return@launchMutation
                    val addressChanged = !addressesEquivalent(
                        current.address,
                        result.details.address,
                    ) || !current.city.equals(result.details.city, ignoreCase = true) ||
                        GeoCoordinates(current.latitude, current.longitude).distanceMilesTo(
                            GeoCoordinates(result.details.latitude, result.details.longitude)
                        ) > LOCATION_CHANGE_THRESHOLD_MILES
                    val resolvedPhone = when {
                        result.details.phoneNumber == null -> current.phoneNumber
                        phonesEquivalent(current.phoneNumber, result.details.phoneNumber) ->
                            current.phoneNumber
                        else -> result.details.phoneNumber
                    }
                    val details = result.details.copy(
                        address = if (addressChanged) result.details.address else current.address,
                        city = if (addressChanged) result.details.city else current.city,
                        latitude = if (addressChanged) result.details.latitude else current.latitude,
                        longitude = if (addressChanged) result.details.longitude else current.longitude,
                        phoneNumber = resolvedPhone,
                        googlePrimaryType = result.details.googlePrimaryType
                            ?: current.googlePrimaryType,
                    )
                    val review = GoogleDetailsReview(
                        details = details,
                        oldAddress = current.address,
                        oldPhoneNumber = current.phoneNumber,
                        addressChanged = addressChanged,
                        phoneChanged = current.phoneNumber != details.phoneNumber,
                        listingIdChanged = current.placeId != details.placeId,
                        listingTypeChanged = current.googlePrimaryType != details.googlePrimaryType,
                    )
                    if (
                        !review.addressChanged && !review.phoneChanged &&
                        !review.listingIdChanged && !review.listingTypeChanged
                    ) {
                        _events.emit(UiEvent.Message("Saved Google details are already current."))
                    } else {
                        _state.update { it.copy(googleDetailsReview = review) }
                    }
                }
            }
        }
    }

    private fun confirmGoogleDetails() {
        val id = favoriteId ?: return
        val review = _state.value.googleDetailsReview ?: return
        launchMutation(PlaceDetailsMutation.GoogleDetails) {
            detailsRepository.updateGoogleDetails(
                id = id,
                placeId = review.details.placeId,
                address = review.details.address,
                city = review.details.city,
                latitude = review.details.latitude,
                longitude = review.details.longitude,
                phoneNumber = review.details.phoneNumber,
                googlePrimaryType = review.details.googlePrimaryType,
            )
            _state.update { it.copy(googleDetailsReview = null) }
            _events.emit(UiEvent.Message("Google details updated."))
        }
    }

    private fun launchMutation(
        mutation: PlaceDetailsMutation,
        operation: suspend () -> Unit,
    ) {
        if (mutationJobs[mutation]?.isActive == true) return
        mutationJobs[mutation] = viewModelScope.launch {
            _state.update { it.copy(pendingMutations = it.pendingMutations + mutation) }
            try {
                operation()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PrivacySafeLog.error(TAG, "Failed to update saved-place field", exception)
                _events.emit(UiEvent.Message("That change could not be saved. Please try again."))
            } finally {
                _state.update { it.copy(pendingMutations = it.pendingMutations - mutation) }
                mutationJobs.remove(mutation)
            }
        }
    }

    sealed interface UiEvent {
        data class Message(val value: String) : UiEvent
        data class OpenMap(val favoriteId: Int) : UiEvent
    }

    private companion object {
        const val TAG = "PlaceDetailsVM"
        const val ACTIVE_EDITOR = "details_active_editor"
        const val DRAFT_NAME = "details_draft_name"
        const val DRAFT_TYPE = "details_draft_type"
        const val DRAFT_NOTES = "details_draft_notes"
        const val LOCATION_CHANGE_THRESHOLD_MILES = 0.01
    }
}

internal fun addressesEquivalent(first: String, second: String): Boolean =
    normalizeAddress(first) == normalizeAddress(second)

private fun normalizeAddress(value: String): String = value
    .trim()
    .lowercase()
    .replace(Regex("(?:,\\s*)?(?:usa|united states(?: of america)?)$"), "")
    .replace(Regex("[.,]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

internal fun phonesEquivalent(first: String?, second: String?): Boolean {
    if (first == null || second == null) return first == second
    val firstDigits = first.filter(Char::isDigit)
    val secondDigits = second.filter(Char::isDigit)
    if (firstDigits == secondDigits) return true
    val comparableLength = minOf(firstDigits.length, secondDigits.length)
    return comparableLength >= 10 &&
        firstDigits.takeLast(comparableLength) == secondDigits.takeLast(comparableLength)
}

private fun com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure.toRefreshMessage(): String =
    when (category) {
        PlaceSearchFailureCategory.Authorization ->
            "Google could not authorize this update. Check the Places API key settings."
        PlaceSearchFailureCategory.Quota ->
            "Google's request limit was reached. Please try again later."
        PlaceSearchFailureCategory.Network ->
            "Connect to the internet and try the Google update again."
        PlaceSearchFailureCategory.NotFound ->
            "Google no longer recognizes this saved listing. Your place was not changed."
        PlaceSearchFailureCategory.InvalidRequest ->
            "This saved Google listing cannot be refreshed. Your place was not changed."
        PlaceSearchFailureCategory.Unavailable,
        PlaceSearchFailureCategory.Unknown,
        -> "Google details are unavailable right now. Please try again."
    }
