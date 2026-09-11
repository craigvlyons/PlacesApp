package com.example.favoriteplaces.feature_favorites.presentation.nearby

import androidx.lifecycle.SavedStateHandle
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.GeoCoordinates
import com.example.favoriteplaces.feature_favorites.domain.model.distanceMilesTo
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyCategory
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyPlace
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbySearchRequest
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceAddressComponent
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlacePrediction
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchOrigin
import com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchResult
import com.example.favoriteplaces.feature_favorites.domain.model.places.SelectedPlace
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.repository.CurrentLocationProvider
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.NearbyPlacesRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.PlaceSearchSession
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
import com.example.favoriteplaces.feature_favorites.domain.use_case.settings.UpdateNearbyTypePickerUseCase
import com.example.favoriteplaces.feature_favorites.presentation.favorites.SavedPlaceTypeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.favoriteplaces.testutil.FakeSavedPlacesPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `saved places are the default and Google discovery stays lazy and cached`() = runTest(dispatcher) {
        val nearby = FakeNearbyRepository()
        val viewModel = createViewModel(nearby = nearby)

        advanceUntilIdle()
        assertEquals(NearbySource.OurPlaces, viewModel.state.value.source)
        assertTrue(nearby.requests.isEmpty())

        viewModel.onEvent(NearbyEvent.LocationPermissionResult(true))
        advanceUntilIdle()
        assertTrue(nearby.requests.isEmpty())

        viewModel.onEvent(NearbyEvent.SelectSource(NearbySource.NewPlaces))
        advanceUntilIdle()
        assertEquals(10, nearby.requests.single().radiusMiles)
        assertEquals(PLACE, viewModel.state.value.discoveryPlaces.single())

        viewModel.onEvent(NearbyEvent.ApplyDiscoveryFilters(15, NearbyCategory.Coffee))
        advanceUntilIdle()
        assertEquals(15, nearby.requests.last().radiusMiles)
        assertEquals(NearbyCategory.Coffee, nearby.requests.last().category)

        viewModel.onEvent(NearbyEvent.SelectSource(NearbySource.OurPlaces))
        viewModel.onEvent(NearbyEvent.SelectSource(NearbySource.NewPlaces))
        advanceUntilIdle()
        assertEquals(2, nearby.requests.size)
    }

    @Test
    fun `saved projection applies the default location boundary before optional filters`() = runTest(dispatcher) {
        val favorites = FakeFavorites(
            listOf(
                savedFavorite(
                    id = 1,
                    title = "Near",
                    latitude = ORIGIN.latitude,
                    longitude = ORIGIN.longitude,
                    isFavorite = true,
                    rating = 4,
                ),
                savedFavorite(id = 2, title = "Far", latitude = 40.7392, color = 22, placeType = null),
                savedFavorite(id = 3, title = "Missing pin", latitude = 0.0, longitude = 0.0),
            )
        )
        val viewModel = createViewModel(favorites = favorites)

        viewModel.onEvent(NearbyEvent.LocationPermissionResult(true))
        advanceUntilIdle()

        assertEquals(listOf("Near"), viewModel.state.value.savedResults.map { it.favorite.title })
        assertEquals(1, viewModel.state.value.skippedCoordinateCount)
        assertEquals(3, viewModel.state.value.savedPlaceCount)
        assertEquals(0.0, viewModel.state.value.savedResults.first().distanceMiles ?: -1.0, 0.001)

        viewModel.onEvent(
            NearbyEvent.ApplySavedFilters(
                SavedNearbyFilters(radiusMiles = 10, favoriteOnly = true, minimumRating = 3)
            )
        )
        advanceUntilIdle()
        assertEquals(listOf("Near"), viewModel.state.value.savedResults.map { it.favorite.title })

        viewModel.onEvent(
            NearbyEvent.ApplySavedFilters(
                SavedNearbyFilters(
                    radiusMiles = 100,
                    placeType = SavedPlaceTypeFilter.NotSet,
                    color = 22,
                )
            )
        )
        advanceUntilIdle()
        assertEquals(listOf("Far"), viewModel.state.value.savedResults.map { it.favorite.title })

        viewModel.onEvent(
            NearbyEvent.ApplySavedFilters(
                SavedNearbyFilters(
                    radiusMiles = 100,
                    placeType = SavedPlaceTypeFilter.Exact("Restaurant"),
                    color = 22,
                )
            )
        )
        advanceUntilIdle()
        assertTrue(viewModel.state.value.savedResults.isEmpty())
    }

    @Test
    fun `saved places require a location before nearby projection`() = runTest(dispatcher) {
        val favorites = FakeFavorites(listOf(savedFavorite(id = 7, title = "Saved")))
        val viewModel = createViewModel(favorites = favorites)

        advanceUntilIdle()

        assertTrue(viewModel.state.value.savedResults.isEmpty())
        assertEquals(1, viewModel.state.value.savedPlaceCount)
        assertEquals(25, viewModel.state.value.savedFilters.radiusMiles)
        assertTrue(viewModel.state.value.needsLocationPermission)
    }

    @Test
    fun `source view and independent filters restore from saved state`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val first = createViewModel(savedStateHandle = handle)
        first.onEvent(NearbyEvent.ApplySavedFilters(SavedNearbyFilters(radiusMiles = 12, favoriteOnly = true)))
        first.onEvent(NearbyEvent.ApplyDiscoveryFilters(15, NearbyCategory.Bakeries))
        first.onEvent(NearbyEvent.ToggleView)
        first.onEvent(NearbyEvent.SelectSource(NearbySource.NewPlaces))
        advanceUntilIdle()

        val restored = createViewModel(savedStateHandle = handle)
        advanceUntilIdle()

        assertEquals(NearbySource.NewPlaces, restored.state.value.source)
        assertTrue(restored.state.value.isMapView)
        assertEquals(12, restored.state.value.savedFilters.radiusMiles)
        assertTrue(restored.state.value.savedFilters.favoriteOnly)
        assertEquals(15, restored.state.value.discoveryRadiusMiles)
        assertEquals(NearbyCategory.Bakeries, restored.state.value.discoveryCategory)
    }

    @Test
    fun `discovery picker additions persist and removing the active type falls back to Any`() =
        runTest(dispatcher) {
            val preferences = FakeSavedPlacesPreferencesRepository()
            val handle = SavedStateHandle()
            val viewModel = createViewModel(preferences = preferences, savedStateHandle = handle)
            advanceUntilIdle()

            viewModel.onEvent(NearbyEvent.AddDiscoveryCategory("ramen_restaurant"))
            advanceUntilIdle()
            assertTrue("ramen_restaurant" in preferences.value.value.nearbyDiscoveryTypeIds)
            assertTrue(
                viewModel.state.value.discoveryPickerCategories.any {
                    it.storageId == "ramen_restaurant"
                }
            )

            val ramen = viewModel.state.value.discoveryPickerCategories.first {
                it.storageId == "ramen_restaurant"
            }
            viewModel.onEvent(NearbyEvent.ApplyDiscoveryFilters(10, ramen))
            viewModel.onEvent(NearbyEvent.RemoveDiscoveryCategory("ramen_restaurant"))
            advanceUntilIdle()

            assertFalse("ramen_restaurant" in preferences.value.value.nearbyDiscoveryTypeIds)
            assertEquals(NearbyCategory.All, viewModel.state.value.discoveryCategory)
            assertEquals(NearbyCategory.ALL_STORAGE_ID, handle.get<String>("nearby_category"))
        }

    @Test
    fun `distance calculator is deterministic at zero and across Denver area`() {
        assertEquals(0.0, ORIGIN.distanceMilesTo(ORIGIN), 0.0001)
        val boulder = GeoCoordinates(40.0150, -105.2705)
        assertEquals(24.3, ORIGIN.distanceMilesTo(boulder), 0.8)
        assertEquals(
            ORIGIN.distanceMilesTo(boulder),
            boulder.distanceMilesTo(ORIGIN),
            0.0001,
        )
        assertTrue(
            GeoCoordinates(0.0, 0.0)
                .distanceMilesTo(GeoCoordinates(0.0, 180.0))
                .isFinite()
        )
    }

    @Test
    fun `late result from a stale discovery key is ignored`() = runTest(dispatcher) {
        val nearby = ControllableNearbyRepository()
        val handle = SavedStateHandle(
            mapOf(
                "nearby_source" to NearbySource.NewPlaces.name,
                "nearby_origin_latitude" to ORIGIN.latitude,
                "nearby_origin_longitude" to ORIGIN.longitude,
            )
        )
        val viewModel = createViewModel(nearby = nearby, savedStateHandle = handle)
        advanceUntilIdle()
        assertEquals(listOf(10), nearby.pending.map { it.first.radiusMiles })

        viewModel.onEvent(NearbyEvent.ApplyDiscoveryFilters(15, NearbyCategory.Coffee))
        advanceUntilIdle()
        assertEquals(listOf(10, 15), nearby.pending.map { it.first.radiusMiles })

        nearby.complete(radius = 15, place = PLACE.copy(displayName = "Current result"))
        advanceUntilIdle()
        assertEquals("Current result", viewModel.state.value.discoveryPlaces.single().displayName)

        nearby.complete(radius = 10, place = PLACE.copy(displayName = "Stale result"))
        advanceUntilIdle()
        assertEquals("Current result", viewModel.state.value.discoveryPlaces.single().displayName)
    }

    @Test
    fun `successful discovery cache survives a different request failure`() = runTest(dispatcher) {
        val nearby = SuccessThenFailureNearbyRepository()
        val handle = SavedStateHandle(
            mapOf(
                "nearby_source" to NearbySource.NewPlaces.name,
                "nearby_origin_latitude" to ORIGIN.latitude,
                "nearby_origin_longitude" to ORIGIN.longitude,
            )
        )
        val viewModel = createViewModel(nearby = nearby, savedStateHandle = handle)
        advanceUntilIdle()
        assertEquals(PLACE, viewModel.state.value.discoveryPlaces.single())

        viewModel.onEvent(NearbyEvent.ApplyDiscoveryFilters(15, NearbyCategory.Coffee))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.discoveryPlaces.isEmpty())
        assertTrue(viewModel.state.value.errorMessage != null)

        viewModel.onEvent(NearbyEvent.ApplyDiscoveryFilters(10, NearbyCategory.All))
        advanceUntilIdle()

        assertEquals(PLACE, viewModel.state.value.discoveryPlaces.single())
        assertNull(viewModel.state.value.errorMessage)
        assertEquals(listOf(10, 15), nearby.requests.map(NearbySearchRequest::radiusMiles))
    }

    @Test
    fun `saving nearby place uses structured city and preserves exact result data`() = runTest(dispatcher) {
        val favorites = FakeFavorites()
        val viewModel = createViewModel(
            favorites = favorites,
            preferences = FakeSavedPlacesPreferencesRepository(
                SavedPlacesPreferences(defaultCardColor = CUSTOM_DEFAULT_COLOR)
            ),
        )
        viewModel.onEvent(NearbyEvent.LocationPermissionResult(true))
        advanceUntilIdle()

        viewModel.onEvent(NearbyEvent.SavePlace(PLACE))
        advanceUntilIdle()

        val saved = favorites.values.value.single()
        assertEquals(PLACE.placeId, saved.placeId)
        assertEquals(PLACE.formattedAddress, saved.address)
        assertEquals("Denver", saved.city)
        assertEquals("Cafe", saved.placeType)
        assertEquals(CUSTOM_DEFAULT_COLOR, saved.color)
        assertTrue(PLACE.placeId in viewModel.state.value.savedPlaceIds)

        viewModel.onEvent(NearbyEvent.SavePlace(PLACE))
        advanceUntilIdle()
        assertEquals(1, favorites.values.value.size)

        viewModel.onEvent(NearbyEvent.ToggleDiscoveryFavorite(PLACE.placeId))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.savedPlaces.getValue(PLACE.placeId).isFavorite)
    }

    @Test
    fun `saving nearby place fetches and stores phone plus durable Google category`() = runTest(dispatcher) {
        val favorites = FakeFavorites()
        val refresh = object : SavedPlaceRefreshRepository {
            override suspend fun refresh(placeId: String) = SavedPlaceRefreshResult.Success(
                GooglePlaceDetails(
                    placeId = placeId,
                    address = PLACE.formattedAddress,
                    city = "Denver",
                    latitude = PLACE.coordinates.latitude,
                    longitude = PLACE.coordinates.longitude,
                    phoneNumber = "303-555-0101",
                    googlePrimaryType = "coffee_shop",
                )
            )
        }
        val viewModel = createViewModel(favorites = favorites, refresh = refresh)

        viewModel.onEvent(NearbyEvent.SavePlace(PLACE))
        advanceUntilIdle()

        val saved = favorites.values.value.single()
        assertEquals("303-555-0101", saved.phoneNumber)
        assertEquals("coffee_shop", saved.googlePrimaryType)
        assertEquals("Cafe", saved.placeType)
    }

    private fun createViewModel(
        nearby: NearbyPlacesRepository = FakeNearbyRepository(),
        favorites: FakeFavorites = FakeFavorites(),
        preferences: FakeSavedPlacesPreferencesRepository = FakeSavedPlacesPreferencesRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        refresh: SavedPlaceRefreshRepository = NO_REFRESH,
    ) = NearbyViewModel(
        locationProvider = CurrentLocationProvider { ORIGIN },
        nearbyRepository = nearby,
        favoriteUseCases = favorites.toUseCases(),
        preferencesRepository = preferences,
        placeSearchRepository = FakePlaceSearchRepository,
        updateNearbyTypePicker = UpdateNearbyTypePickerUseCase(preferences),
        savedPlaceRefreshRepository = refresh,
        savedStateHandle = savedStateHandle,
    )

    private class FakeNearbyRepository : NearbyPlacesRepository {
        val requests = mutableListOf<NearbySearchRequest>()
        override suspend fun search(request: NearbySearchRequest): PlaceSearchResult<List<NearbyPlace>> {
            requests += request
            return PlaceSearchResult.Success(listOf(PLACE))
        }
    }

    private class ControllableNearbyRepository : NearbyPlacesRepository {
        val pending = mutableListOf<Pair<NearbySearchRequest, Continuation<PlaceSearchResult<List<NearbyPlace>>>>>()

        override suspend fun search(request: NearbySearchRequest): PlaceSearchResult<List<NearbyPlace>> =
            suspendCoroutine { continuation -> pending += request to continuation }

        fun complete(radius: Int, place: NearbyPlace) {
            val entry = pending.first { it.first.radiusMiles == radius }
            entry.second.resume(PlaceSearchResult.Success(listOf(place)))
        }
    }

    private class SuccessThenFailureNearbyRepository : NearbyPlacesRepository {
        val requests = mutableListOf<NearbySearchRequest>()

        override suspend fun search(
            request: NearbySearchRequest,
        ): PlaceSearchResult<List<NearbyPlace>> {
            requests += request
            return if (request.radiusMiles == 10) {
                PlaceSearchResult.Success(listOf(PLACE))
            } else {
                PlaceSearchResult.Failure(
                    com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailure(
                        com.example.favoriteplaces.feature_favorites.domain.model.places.PlaceSearchFailureCategory.Network,
                        retryable = true,
                    )
                )
            }
        }
    }

    private class FakeFavorites(initial: List<Favorite> = emptyList()) : FavoriteRepository {
        val values = MutableStateFlow(initial)
        override fun getFavorites(): Flow<List<Favorite>> = values
        override suspend fun getFavoriteById(id: Int) = values.value.firstOrNull { it.id == id }
        override suspend fun insertFavorite(favorite: Favorite) {
            val nextId = (values.value.mapNotNull(Favorite::id).maxOrNull() ?: 0) + 1
            values.value += favorite.copy(id = favorite.id ?: nextId)
        }
        override suspend fun updateIsFavorite(id: Int, isFavorite: Boolean) {
            values.value = values.value.map { favorite ->
                if (favorite.id == id) favorite.copy(isFavorite = isFavorite) else favorite
            }
        }
        override suspend fun deleteFavorite(favorite: Favorite) = Unit
        override fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<Favorite>> = MutableStateFlow(emptyList())
    }

    private fun FavoriteRepository.toUseCases() = FavoriteUseCases(
        GetFavoriteUseCase(this), GetFavoritesUseCase(this), DeleteFavoriteUseCase(this),
        UpdateIsFavorite(this), AddFavoriteUseCase(this), GetFavoritesByCityAndColorUseCase(this),
    )

    private object FakePlaceSearchRepository : PlaceSearchRepository {
        override fun createSession() = object : PlaceSearchSession {
            override suspend fun search(query: String, origin: PlaceSearchOrigin?) =
                PlaceSearchResult.Success(emptyList<PlacePrediction>())
            override suspend fun select(placeId: String): PlaceSearchResult<SelectedPlace> = error("unused")
            override fun abandon() = Unit
        }
    }

    private companion object {
        val NO_REFRESH = object : SavedPlaceRefreshRepository {
            override suspend fun refresh(placeId: String): SavedPlaceRefreshResult =
                SavedPlaceRefreshResult.Failure(
                    PlaceSearchFailure(PlaceSearchFailureCategory.Unavailable, true)
                )
        }
        const val CUSTOM_DEFAULT_COLOR = -12_816_751
        val ORIGIN = GeoCoordinates(39.7392, -104.9903)
        val PLACE = NearbyPlace(
            placeId = "nearby-1",
            displayName = "Nearby Café",
            formattedAddress = "1 Main St, Denver, CO",
            coordinates = GeoCoordinates(39.74, -104.98),
            primaryType = "cafe",
            addressComponents = listOf(
                PlaceAddressComponent("Denver", "Denver", setOf("locality"))
            ),
            thirdPartyAttributions = emptyList(),
            distanceMiles = 0.8,
        )

        fun savedFavorite(
            id: Int,
            title: String,
            latitude: Double = 39.75,
            longitude: Double = -104.99,
            color: Int = 11,
            placeType: String? = "Restaurant",
            isFavorite: Boolean = false,
            rating: Int = 0,
        ) = Favorite(
            id = id,
            placeId = "saved-$id",
            title = title,
            address = "$id Main St",
            content = "notes",
            rating = rating,
            isFavorite = isFavorite,
            color = color,
            city = "Denver",
            latitude = latitude,
            longitude = longitude,
            placeType = placeType,
        )
    }
}
