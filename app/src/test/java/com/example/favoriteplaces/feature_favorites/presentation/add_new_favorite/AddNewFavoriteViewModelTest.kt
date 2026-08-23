package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite

import androidx.lifecycle.SavedStateHandle
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceAddressComponent
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchOrigin
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.SelectedPlace
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.repository.CurrentLocationProvider
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchSession
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.AddFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.DeleteFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesByCityAndColorUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.UpdateIsFavorite
import com.example.favoriteplaces.feature_favorites.presentation.util.AddressResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.favoriteplaces.testutil.FakeSavedPlacesPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AddNewFavoriteViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search omits location bias when current location is unavailable`() = runTest(dispatcher) {
        val searchRepository = FakePlaceSearchRepository()
        val viewModel = createViewModel(
            searchRepository = searchRepository,
            location = null,
        )
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("Root Down"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()

        assertNull(searchRepository.sessions.single().origin)
        assertEquals(PREDICTION, viewModel.uiState.value.predictions.single())
    }

    @Test
    fun `search uses a real fifteen mile bias when location is available`() = runTest(dispatcher) {
        val searchRepository = FakePlaceSearchRepository()
        val viewModel = createViewModel(
            searchRepository = searchRepository,
            location = GeoCoordinates(38.8339, -104.8214),
        )
        viewModel.onLocationPermissionResult(true)
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("coffee"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()

        assertEquals(
            PlaceSearchOrigin(38.8339, -104.8214, 24_140.16),
            searchRepository.sessions.single().origin,
        )
    }

    @Test
    fun `selected SDK details save exact address coordinates and structured city`() = runTest(dispatcher) {
        val favorites = FakeFavoriteRepository()
        val searchRepository = FakePlaceSearchRepository()
        val viewModel = createViewModel(
            searchRepository,
            favorites = favorites,
            preferences = FakeSavedPlacesPreferencesRepository(
                SavedPlacesPreferences(defaultCardColor = CUSTOM_DEFAULT_COLOR)
            ),
        )
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("Root Down"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()
        viewModel.onEvent(AddNewFavoriteEvent.SelectedResult(PREDICTION))
        advanceUntilIdle()
        viewModel.onEvent(AddNewFavoriteEvent.SaveFavorite)
        advanceUntilIdle()

        val saved = favorites.favorites.value.single()
        assertEquals("place-1", saved.placeId)
        assertEquals("Root Down", saved.title)
        assertEquals("1600 W 33rd Ave, Denver, CO 80211, USA", saved.address)
        assertEquals("Denver", saved.city)
        assertEquals(39.7631, saved.latitude, 0.0)
        assertEquals(-105.0201, saved.longitude, 0.0)
        assertEquals("Restaurant", saved.placeType)
        assertEquals(CUSTOM_DEFAULT_COLOR, saved.color)
        assertEquals(
            listOf("Listings by <a href=\"https://example.test\">Example</a>"),
            viewModel.uiState.value.thirdPartyAttributions,
        )
    }

    @Test
    fun `missing structured city never falls back to comma parsing`() = runTest(dispatcher) {
        val favorites = FakeFavoriteRepository()
        val searchRepository = FakePlaceSearchRepository(
            selectedPlace = SELECTED_PLACE.copy(addressComponents = emptyList())
        )
        val viewModel = createViewModel(searchRepository, favorites = favorites)
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("Root Down"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()
        viewModel.onEvent(AddNewFavoriteEvent.SelectedResult(PREDICTION))
        advanceUntilIdle()
        viewModel.onEvent(AddNewFavoriteEvent.SaveFavorite)
        advanceUntilIdle()

        assertTrue(favorites.favorites.value.isEmpty())
    }

    @Test
    fun `rapid save taps insert the selected place only once`() = runTest(dispatcher) {
        val favorites = FakeFavoriteRepository()
        val searchRepository = FakePlaceSearchRepository()
        val viewModel = createViewModel(searchRepository, favorites = favorites)
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("Root Down"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()
        viewModel.onEvent(AddNewFavoriteEvent.SelectedResult(PREDICTION))
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.SaveFavorite)
        viewModel.onEvent(AddNewFavoriteEvent.SaveFavorite)
        advanceUntilIdle()

        assertEquals(1, favorites.insertCount)
        assertEquals(1, favorites.favorites.value.size)
    }

    @Test
    fun `a new search abandons the prior billing session`() = runTest(dispatcher) {
        val searchRepository = FakePlaceSearchRepository()
        val viewModel = createViewModel(searchRepository)
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("first"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()
        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("second"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()

        assertTrue(searchRepository.sessions.first().abandoned)
        assertEquals("second", searchRepository.sessions.last().query)
    }

    @Test
    fun `editing a searched query immediately abandons its billing session`() = runTest(dispatcher) {
        val searchRepository = FakePlaceSearchRepository()
        val viewModel = createViewModel(searchRepository)
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("first"))
        viewModel.onEvent(AddNewFavoriteEvent.Search)
        advanceUntilIdle()

        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch("first changed"))

        assertTrue(searchRepository.sessions.single().abandoned)
        assertTrue(viewModel.uiState.value.predictions.isEmpty())
        assertNull(viewModel.uiState.value.selectedPlaceId)
    }

    @Test
    fun `query restores without restoring a Places session or selection`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("add_search_query" to "Café near me"))
        val repository = FakePlaceSearchRepository()
        val viewModel = createViewModel(repository, savedStateHandle = handle)
        advanceUntilIdle()

        assertEquals("Café near me", viewModel.uiState.value.searchField.text)
        assertNull(viewModel.uiState.value.selectedPlaceId)
        assertTrue(viewModel.uiState.value.predictions.isEmpty())
        assertTrue(repository.sessions.isEmpty())
    }

    private fun createViewModel(
        searchRepository: FakePlaceSearchRepository,
        favorites: FakeFavoriteRepository = FakeFavoriteRepository(),
        location: GeoCoordinates? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        preferences: FakeSavedPlacesPreferencesRepository = FakeSavedPlacesPreferencesRepository(),
    ): AddNewFavoriteViewModel = AddNewFavoriteViewModel(
        currentLocationProvider = CurrentLocationProvider { location },
        addressResolver = object : AddressResolver {
            override suspend fun resolve(latitude: Double, longitude: Double): String? = null
        },
        placeSearchRepository = searchRepository,
        favoriteUseCases = favorites.toUseCases(),
        preferencesRepository = preferences,
        savedStateHandle = savedStateHandle,
    )

    private class FakePlaceSearchRepository(
        private val selectedPlace: SelectedPlace = SELECTED_PLACE,
    ) : PlaceSearchRepository {
        val sessions = mutableListOf<FakePlaceSearchSession>()

        override fun createSession(): PlaceSearchSession = FakePlaceSearchSession(selectedPlace)
            .also(sessions::add)
    }

    private class FakePlaceSearchSession(
        private val selectedPlace: SelectedPlace,
    ) : PlaceSearchSession {
        var query: String? = null
        var origin: PlaceSearchOrigin? = null
        var abandoned = false

        override suspend fun search(
            query: String,
            origin: PlaceSearchOrigin?,
        ): PlaceSearchResult<List<PlacePrediction>> {
            this.query = query
            this.origin = origin
            return PlaceSearchResult.Success(listOf(PREDICTION))
        }

        override suspend fun select(placeId: String): PlaceSearchResult<SelectedPlace> =
            PlaceSearchResult.Success(selectedPlace)

        override fun abandon() {
            abandoned = true
        }
    }

    private class FakeFavoriteRepository : FavoriteRepository {
        val favorites = MutableStateFlow<List<Favorite>>(emptyList())
        var insertCount = 0

        override fun getFavorites(): Flow<List<Favorite>> = favorites
        override suspend fun getFavoriteById(id: Int): Favorite? =
            favorites.value.firstOrNull { it.id == id }
        override suspend fun insertFavorite(favorite: Favorite) {
            insertCount += 1
            favorites.value += favorite
        }
        override suspend fun updateIsFavorite(id: Int, isFavorite: Boolean) = Unit
        override suspend fun deleteFavorite(favorite: Favorite) = Unit
        override fun getFavoritesByCityAndColor(
            city: String,
            color: Int,
        ): Flow<List<Favorite>> = MutableStateFlow(emptyList())
    }

    private fun FavoriteRepository.toUseCases(): FavoriteUseCases = FavoriteUseCases(
        getFavorite = GetFavoriteUseCase(this),
        getFavorites = GetFavoritesUseCase(this),
        deleteFavorite = DeleteFavoriteUseCase(this),
        updateIsFavorite = UpdateIsFavorite(this),
        addFavorite = AddFavoriteUseCase(this),
        getFavoritesByCityAndColor = GetFavoritesByCityAndColorUseCase(this),
    )

    private companion object {
        const val CUSTOM_DEFAULT_COLOR = -12_816_751
        val PREDICTION = PlacePrediction(
            placeId = "place-1",
            primaryText = "Root Down",
            secondaryText = "Denver, CO",
            types = listOf("restaurant"),
            distanceMeters = null,
        )
        val SELECTED_PLACE = SelectedPlace(
            placeId = "place-1",
            displayName = "Root Down",
            formattedAddress = "1600 W 33rd Ave, Denver, CO 80211, USA",
            latitude = 39.7631,
            longitude = -105.0201,
            addressComponents = listOf(
                PlaceAddressComponent("Denver", "Denver", setOf("locality"))
            ),
            thirdPartyAttributions = listOf(
                "Listings by <a href=\"https://example.test\">Example</a>"
            ),
            placeType = "Restaurant",
        )
    }
}
