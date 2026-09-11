package com.example.favoriteplaces.feature_favorites.presentation.place_details

import androidx.lifecycle.SavedStateHandle
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteDetailsRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceRefreshResult
import com.example.favoriteplaces.feature_favorites.domain.repository.GooglePlaceDetails
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.AddFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.DeleteFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesByCityAndColorUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.UpdateIsFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `observes the requested Room record and reports deletion`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE)
        val viewModel = repository.viewModel()
        advanceUntilIdle()

        assertEquals(FAVORITE, viewModel.state.value.favorite)
        assertFalse(viewModel.state.value.isLoading)

        repository.value.value = null
        advanceUntilIdle()

        assertNull(viewModel.state.value.favorite)
        assertEquals("This saved place is no longer available.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `focused sheets update only their owned fields`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE)
        val viewModel = repository.viewModel()
        advanceUntilIdle()

        viewModel.onEvent(PlaceDetailsEvent.OpenNameAndTypeEditor)
        viewModel.onEvent(PlaceDetailsEvent.EnterName("  Updated place  "))
        viewModel.onEvent(PlaceDetailsEvent.EnterType("  Cocktail  bar "))
        repository.value.value = repository.value.value?.copy(isFavorite = false, rating = 5)
        viewModel.onEvent(PlaceDetailsEvent.SaveNameAndType)
        advanceUntilIdle()

        assertEquals("Updated place", repository.value.value?.title)
        assertEquals("Cocktail bar", repository.value.value?.placeType)
        assertEquals(false, repository.value.value?.isFavorite)
        assertEquals(5, repository.value.value?.rating)

        viewModel.onEvent(PlaceDetailsEvent.EnterNotes("Line one\nLine two ☕"))
        viewModel.onEvent(PlaceDetailsEvent.SaveNotes)
        advanceUntilIdle()

        assertEquals("Line one\nLine two ☕", repository.value.value?.content)
        assertEquals(FAVORITE.address, repository.value.value?.address)
        assertEquals(FAVORITE.color, repository.value.value?.color)
    }

    @Test
    fun `quick actions update isolated fields and selected rating can clear`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE)
        val viewModel = repository.viewModel()
        advanceUntilIdle()

        viewModel.onEvent(PlaceDetailsEvent.SelectColor(-99))
        viewModel.onEvent(PlaceDetailsEvent.SelectRating(4))
        viewModel.onEvent(PlaceDetailsEvent.ToggleFavorite)
        advanceUntilIdle()

        assertEquals(-99, repository.value.value?.color)
        assertEquals(4, repository.value.value?.rating)
        assertEquals(false, repository.value.value?.isFavorite)
        assertEquals(FAVORITE.content, repository.value.value?.content)

        viewModel.onEvent(PlaceDetailsEvent.SelectRating(0))
        advanceUntilIdle()
        assertNull(repository.value.value?.rating)
    }

    @Test
    fun `restored inline notes draft remains unsaved until Save is requested`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE)
        val handle = SavedStateHandle(
            mapOf(
                "favoriteId" to FAVORITE.id,
                "details_draft_notes" to "Unsaved draft",
            )
        )
        val viewModel = PlaceDetailsViewModel(
            repository,
            repository.toUseCases(),
            NO_REFRESH,
            handle,
        )
        advanceUntilIdle()

        assertNull(viewModel.state.value.activeEditor)
        assertEquals("Unsaved draft", viewModel.state.value.draftNotes)
        assertEquals(FAVORITE.content, repository.value.value?.content)

        viewModel.onEvent(PlaceDetailsEvent.SaveNotes)
        advanceUntilIdle()
        assertEquals("Unsaved draft", repository.value.value?.content)
    }

    @Test
    fun `inline notes draft survives unrelated Room emissions`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE)
        val viewModel = repository.viewModel()
        advanceUntilIdle()

        viewModel.onEvent(PlaceDetailsEvent.EnterNotes("Unsaved local draft"))
        repository.value.value = repository.value.value?.copy(rating = 5)
        advanceUntilIdle()

        assertEquals("Unsaved local draft", viewModel.state.value.draftNotes)
        assertEquals(FAVORITE.content, repository.value.value?.content)
        assertEquals(5, viewModel.state.value.favorite?.rating)
    }

    @Test
    fun `failed inline notes save retains draft and Room truth`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE, failNotes = true)
        val viewModel = repository.viewModel()
        advanceUntilIdle()
        val message = async { viewModel.events.first() }

        viewModel.onEvent(PlaceDetailsEvent.EnterNotes("Retry this draft"))
        viewModel.onEvent(PlaceDetailsEvent.SaveNotes)
        advanceUntilIdle()

        assertEquals("Retry this draft", viewModel.state.value.draftNotes)
        assertEquals(FAVORITE.content, repository.value.value?.content)
        assertEquals("That change could not be saved. Please try again.", message.await().let {
            (it as PlaceDetailsViewModel.UiEvent.Message).value
        })
        assertTrue(viewModel.state.value.pendingMutations.isEmpty())
    }

    @Test
    fun `duplicate quick action is suppressed while its first write is pending`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRepository(FAVORITE, colorGate = gate)
        val viewModel = repository.viewModel()
        advanceUntilIdle()

        viewModel.onEvent(PlaceDetailsEvent.SelectColor(-99))
        dispatcher.scheduler.runCurrent()
        viewModel.onEvent(PlaceDetailsEvent.SelectColor(-100))
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.colorCalls)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(-99, repository.value.value?.color)
    }

    @Test
    fun `failed quick action preserves Room truth and emits a retryable message`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE, failColor = true)
        val viewModel = repository.viewModel()
        advanceUntilIdle()
        val message = async { viewModel.events.first() }

        viewModel.onEvent(PlaceDetailsEvent.SelectColor(-99))
        advanceUntilIdle()

        assertEquals(FAVORITE.color, repository.value.value?.color)
        assertEquals("That change could not be saved. Please try again.", message.await().let {
            (it as PlaceDetailsViewModel.UiEvent.Message).value
        })
        assertTrue(viewModel.state.value.pendingMutations.isEmpty())
    }

    @Test
    fun `Google refresh requires review and updates only Google-owned fields`() = runTest(dispatcher) {
        val repository = FakeRepository(FAVORITE)
        val refreshed = GooglePlaceDetails(
            placeId = "canonical-place-7",
            address = "77 Updated Ave, Boulder, CO",
            city = "Boulder",
            latitude = 40.01,
            longitude = -105.27,
            phoneNumber = "303-555-0177",
        )
        val viewModel = repository.viewModel(
            object : SavedPlaceRefreshRepository {
                override suspend fun refresh(placeId: String) =
                    SavedPlaceRefreshResult.Success(refreshed)
            }
        )
        advanceUntilIdle()

        viewModel.onEvent(PlaceDetailsEvent.CheckGoogleDetails)
        advanceUntilIdle()

        assertEquals(FAVORITE, repository.value.value)
        assertEquals(refreshed, viewModel.state.value.googleDetailsReview?.details)

        viewModel.onEvent(PlaceDetailsEvent.ConfirmGoogleDetails)
        advanceUntilIdle()

        val saved = repository.value.value ?: error("fixture missing")
        assertEquals(refreshed.address, saved.address)
        assertEquals(refreshed.phoneNumber, saved.phoneNumber)
        assertEquals(refreshed.placeId, saved.placeId)
        assertEquals(FAVORITE.title, saved.title)
        assertEquals(FAVORITE.placeType, saved.placeType)
        assertEquals(FAVORITE.content, saved.content)
        assertEquals(FAVORITE.color, saved.color)
        assertEquals(FAVORITE.rating, saved.rating)
        assertEquals(FAVORITE.isFavorite, saved.isFavorite)
        assertNull(viewModel.state.value.googleDetailsReview)
    }

    @Test
    fun `Google response without phone never erases saved phone`() = runTest(dispatcher) {
        val original = FAVORITE.copy(phoneNumber = "303-555-0100")
        val repository = FakeRepository(original)
        val viewModel = repository.viewModel(
            object : SavedPlaceRefreshRepository {
                override suspend fun refresh(placeId: String) = SavedPlaceRefreshResult.Success(
                    GooglePlaceDetails(
                        placeId = original.placeId!!,
                        address = "Updated address",
                        city = original.city,
                        latitude = original.latitude,
                        longitude = original.longitude,
                        phoneNumber = null,
                    )
                )
            }
        )
        advanceUntilIdle()

        viewModel.onEvent(PlaceDetailsEvent.CheckGoogleDetails)
        advanceUntilIdle()
        viewModel.onEvent(PlaceDetailsEvent.ConfirmGoogleDetails)
        advanceUntilIdle()

        assertEquals("303-555-0100", repository.value.value?.phoneNumber)
    }

    @Test
    fun `harmless Google formatting differences do not create an update review`() = runTest(dispatcher) {
        val original = FAVORITE.copy(
            address = "7 Main St., Denver, CO, USA",
            phoneNumber = "+1 (303) 555-0100",
        )
        val repository = FakeRepository(original)
        val viewModel = repository.viewModel(
            object : SavedPlaceRefreshRepository {
                override suspend fun refresh(placeId: String) = SavedPlaceRefreshResult.Success(
                    GooglePlaceDetails(
                        placeId = placeId,
                        address = "7 Main St, Denver, CO",
                        city = "denver",
                        latitude = original.latitude + 0.00001,
                        longitude = original.longitude,
                        phoneNumber = "(303) 555-0100",
                    )
                )
            }
        )
        advanceUntilIdle()

        viewModel.onEvent(PlaceDetailsEvent.CheckGoogleDetails)
        advanceUntilIdle()

        assertNull(viewModel.state.value.googleDetailsReview)
        assertEquals(original, repository.value.value)
    }

    @Test
    fun `address and phone equivalence ignore only presentation differences`() {
        assertTrue(addressesEquivalent("7 Main St., Denver, CO, USA", "7 Main St, Denver, CO"))
        assertFalse(addressesEquivalent("7 Main St, Denver, CO", "77 Main St, Denver, CO"))
        assertTrue(phonesEquivalent("+1 (303) 555-0100", "303-555-0100"))
        assertFalse(phonesEquivalent("303-555-0100", "303-555-0199"))
    }

    private class FakeRepository(
        initial: Favorite?,
        private val failColor: Boolean = false,
        private val failNotes: Boolean = false,
        private val colorGate: CompletableDeferred<Unit>? = null,
    ) : FavoriteRepository, FavoriteDetailsRepository {
        val value = MutableStateFlow(initial)
        var colorCalls = 0

        override fun observeFavoriteById(id: Int): Flow<Favorite?> =
            value.map { it?.takeIf { favorite -> favorite.id == id } }
        override suspend fun updateNameAndType(id: Int, title: String, placeType: String?) {
            value.value = value.value?.takeIf { it.id == id }?.copy(title = title, placeType = placeType)
        }
        override suspend fun updateNotes(id: Int, notes: String?) {
            if (failNotes) error("fixture write failure")
            value.value = value.value?.takeIf { it.id == id }?.copy(content = notes)
        }
        override suspend fun updateColor(id: Int, color: Int) {
            colorCalls += 1
            if (failColor) error("fixture write failure")
            colorGate?.await()
            value.value = value.value?.takeIf { it.id == id }?.copy(color = color)
        }
        override suspend fun updateRating(id: Int, rating: Int?) {
            value.value = value.value?.takeIf { it.id == id }?.copy(rating = rating)
        }
        override suspend fun updateGoogleDetails(
            id: Int,
            placeId: String,
            address: String,
            city: String,
            latitude: Double,
            longitude: Double,
            phoneNumber: String?,
            googlePrimaryType: String?,
        ) {
            value.value = value.value?.takeIf { it.id == id }?.copy(
                placeId = placeId,
                address = address,
                city = city,
                latitude = latitude,
                longitude = longitude,
                phoneNumber = phoneNumber,
                googlePrimaryType = googlePrimaryType,
            )
        }
        override suspend fun fillMissingGoogleMetadata(
            id: Int,
            phoneNumber: String?,
            googlePrimaryType: String?,
        ) {
            value.value = value.value?.takeIf { it.id == id }?.let { favorite ->
                favorite.copy(
                    phoneNumber = favorite.phoneNumber ?: phoneNumber,
                    googlePrimaryType = favorite.googlePrimaryType ?: googlePrimaryType,
                )
            }
        }
        override fun getFavorites(): Flow<List<Favorite>> = value.map(::listOfNotNull)
        override suspend fun getFavoriteById(id: Int) = value.value?.takeIf { it.id == id }
        override suspend fun insertFavorite(favorite: Favorite) { value.value = favorite }
        override suspend fun updateIsFavorite(id: Int, isFavorite: Boolean) {
            value.value = value.value?.takeIf { it.id == id }?.copy(isFavorite = isFavorite)
        }
        override suspend fun deleteFavorite(favorite: Favorite) { value.value = null }
        override fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<Favorite>> =
            getFavorites().map { items -> items.filter { it.city == city && it.color == color } }
    }

    private fun FakeRepository.viewModel(
        refreshRepository: SavedPlaceRefreshRepository = NO_REFRESH,
    ) = PlaceDetailsViewModel(
        detailsRepository = this,
        favoriteUseCases = toUseCases(),
        refreshRepository = refreshRepository,
        savedStateHandle = SavedStateHandle(mapOf("favoriteId" to FAVORITE.id)),
    )

    private fun FavoriteRepository.toUseCases() = FavoriteUseCases(
        GetFavoriteUseCase(this), GetFavoritesUseCase(this), DeleteFavoriteUseCase(this),
        UpdateIsFavorite(this), AddFavoriteUseCase(this), GetFavoritesByCityAndColorUseCase(this),
    )

    private companion object {
        val NO_REFRESH = object : SavedPlaceRefreshRepository {
            override suspend fun refresh(placeId: String): SavedPlaceRefreshResult =
                SavedPlaceRefreshResult.Failure(
                    PlaceSearchFailure(PlaceSearchFailureCategory.Unavailable, true)
                )
        }
        val FAVORITE = Favorite(
            id = 7,
            placeId = "place-7",
            title = "Original",
            address = "7 Main St",
            content = "Original notes",
            rating = 2,
            isFavorite = true,
            color = -7,
            city = "Denver",
            latitude = 39.7,
            longitude = -104.9,
            placeType = "Restaurant",
        )
    }
}
