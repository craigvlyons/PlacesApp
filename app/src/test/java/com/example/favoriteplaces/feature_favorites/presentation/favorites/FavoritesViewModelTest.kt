package com.example.favoriteplaces.feature_favorites.presentation.favorites

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.model.settings.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.domain.model.settings.OrderType
import com.example.favoriteplaces.feature_favorites.domain.model.settings.SavedPlacesPreferences
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteDetailsRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadata
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadataRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlaceActionMetadataResult
import com.example.favoriteplaces.feature_favorites.domain.repository.SavedPlacesPreferencesRepository
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.AddFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.DeleteFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesByCityAndColorUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.UpdateIsFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `order and view changes persist without resubscribing to Room`() = runTest(dispatcher) {
        val favorites = FakeFavoriteRepository()
        val preferences = FakePreferencesRepository()
        val viewModel = favorites.createViewModel(preferences)
        advanceUntilIdle()

        assertEquals(1, favorites.subscriptionCount)
        assertEquals(listOf(1, 2), viewModel.state.value.favorites.map { it.id })

        viewModel.onEvent(FavoritesEvent.Order(FavoriteOrder.Rating(OrderType.Descending)))
        viewModel.onEvent(FavoritesEvent.ToggleListOrCardView)
        advanceUntilIdle()

        assertEquals(1, favorites.subscriptionCount)
        assertEquals(listOf(1, 2), viewModel.state.value.favorites.map { it.id })
        assertTrue(viewModel.state.value.isListView)
        assertEquals(FavoriteOrder.Rating(OrderType.Descending), preferences.value.value.favoriteOrder)
    }

    @Test
    fun `heart state follows repository truth`() = runTest(dispatcher) {
        val favorites = FakeFavoriteRepository()
        val viewModel = favorites.createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(FavoritesEvent.LovedFavorite(1, true))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.favorites.first { it.id == 1 }.isFavorite)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `type and type not set filters combine without another Room subscription`() = runTest(dispatcher) {
        val favorites = FakeFavoriteRepository()
        val viewModel = favorites.createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("Restaurant"), viewModel.state.value.availablePlaceTypes)
        assertTrue(viewModel.state.value.hasPlacesWithoutType)

        viewModel.onEvent(
            FavoritesEvent.ApplyFilters(
                SavedPlacesFilters(placeType = SavedPlaceTypeFilter.Exact("restaurant"))
            )
        )
        advanceUntilIdle()
        assertEquals(listOf(1), viewModel.state.value.favorites.map { it.id })

        viewModel.onEvent(
            FavoritesEvent.ApplyFilters(
                SavedPlacesFilters(
                    city = "Austin",
                    placeType = SavedPlaceTypeFilter.NotSet,
                )
            )
        )
        advanceUntilIdle()
        assertEquals(listOf(2), viewModel.state.value.favorites.map { it.id })
        assertEquals(1, favorites.subscriptionCount)
    }

    @Test
    fun `failed saved-place observation can be retried without recreating the screen`() =
        runTest(dispatcher) {
            val favorites = FakeFavoriteRepository(shouldFail = true)
            val viewModel = favorites.createViewModel()
            advanceUntilIdle()

            assertEquals("Saved places could not be loaded.", viewModel.state.value.errorMessage)

            favorites.shouldFail = false
            viewModel.onEvent(FavoritesEvent.RetryLoading)
            advanceUntilIdle()

            assertEquals(listOf(1, 2), viewModel.state.value.favorites.map { it.id })
            assertEquals(2, favorites.subscriptionCount)
        }

    @Test
    fun `opening actions lazily fetches and persists missing phone metadata once`() =
        runTest(dispatcher) {
            val favorites = FakeFavoriteRepository()
            val actions = FakeActionMetadataRepository()
            val viewModel = favorites.createViewModel(actionMetadataRepository = actions)
            advanceUntilIdle()

            val favorite = viewModel.state.value.favorites.first { it.id == 1 }
            viewModel.loadPlaceActionsIfNeeded(favorite)
            advanceUntilIdle()

            assertEquals(1, actions.requests)
            assertEquals(
                "303-555-0123",
                viewModel.state.value.favorites.first { it.id == 1 }.phoneNumber,
            )
            assertEquals(
                "restaurant",
                viewModel.state.value.favorites.first { it.id == 1 }.googlePrimaryType,
            )

            viewModel.loadPlaceActionsIfNeeded(favorite)
            advanceUntilIdle()
            assertEquals(1, actions.requests)
        }

    private class FakePreferencesRepository : SavedPlacesPreferencesRepository {
        val value = MutableStateFlow(SavedPlacesPreferences())
        override val preferences: Flow<SavedPlacesPreferences> = value
        override suspend fun setListView(isListView: Boolean) {
            value.value = value.value.copy(isListView = isListView)
        }
        override suspend fun setFavoriteOrder(favoriteOrder: FavoriteOrder) {
            value.value = value.value.copy(favoriteOrder = favoriteOrder)
        }
        override suspend fun setAppColor(color: Int) {
            value.value = value.value.copy(appColor = color)
        }
        override suspend fun setDefaultCardColor(color: Int) {
            value.value = value.value.copy(defaultCardColor = color)
        }
        override suspend fun setNearbyDiscoveryTypeIds(typeIds: List<String>) {
            value.value = value.value.copy(nearbyDiscoveryTypeIds = typeIds)
        }
    }

    private class FakeFavoriteRepository(
        var shouldFail: Boolean = false,
    ) : FavoriteRepository, FavoriteDetailsRepository {
        private val value = MutableStateFlow(
            listOf(
                Favorite(1, "1", "One", "Address", null, 5, false, -1, "Denver", 1.0, 1.0, "Restaurant"),
                Favorite(2, "2", "Two", "Address", null, 2, false, -2, "Austin", 2.0, 2.0),
            )
        )
        var subscriptionCount = 0
        override fun getFavorites(): Flow<List<Favorite>> = flow {
            subscriptionCount += 1
            if (shouldFail) error("fixture read failure")
            emitAll(value)
        }
        override suspend fun getFavoriteById(id: Int) = value.value.firstOrNull { it.id == id }
        override suspend fun insertFavorite(favorite: Favorite) {
            value.value = value.value.filterNot { it.id == favorite.id } + favorite
        }
        override suspend fun updateIsFavorite(id: Int, isFavorite: Boolean) {
            value.value = value.value.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }
        override suspend fun deleteFavorite(favorite: Favorite) { value.value -= favorite }
        override fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<Favorite>> =
            MutableStateFlow(value.value.filter { it.city == city && it.color == color })

        override fun observeFavoriteById(id: Int): Flow<Favorite?> =
            value.map { favorites -> favorites.firstOrNull { it.id == id } }
        override suspend fun updateNameAndType(id: Int, title: String, placeType: String?) =
            update(id) { it.copy(title = title, placeType = placeType) }
        override suspend fun updateNotes(id: Int, notes: String?) =
            update(id) { it.copy(content = notes) }
        override suspend fun updateColor(id: Int, color: Int) = update(id) { it.copy(color = color) }
        override suspend fun updateRating(id: Int, rating: Int?) =
            update(id) { it.copy(rating = rating) }
        override suspend fun updateGoogleDetails(
            id: Int,
            placeId: String,
            address: String,
            city: String,
            latitude: Double,
            longitude: Double,
            phoneNumber: String?,
            googlePrimaryType: String?,
        ) = update(id) {
            it.copy(
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
        ) = update(id) {
            it.copy(
                phoneNumber = it.phoneNumber ?: phoneNumber,
                googlePrimaryType = it.googlePrimaryType ?: googlePrimaryType,
            )
        }

        private fun update(id: Int, transform: (Favorite) -> Favorite) {
            value.value = value.value.map { if (it.id == id) transform(it) else it }
        }
    }

    private class FakeActionMetadataRepository : SavedPlaceActionMetadataRepository {
        var requests = 0
        override suspend fun fetch(placeId: String): SavedPlaceActionMetadataResult {
            requests += 1
            return SavedPlaceActionMetadataResult.Success(
                SavedPlaceActionMetadata("303-555-0123", "restaurant")
            )
        }
    }

    private fun FakeFavoriteRepository.createViewModel(
        preferences: SavedPlacesPreferencesRepository = FakePreferencesRepository(),
        actionMetadataRepository: SavedPlaceActionMetadataRepository = FakeActionMetadataRepository(),
    ) = FavoritesViewModel(
        favoriteUseCases = toUseCases(),
        preferencesRepository = preferences,
        detailsRepository = this,
        actionMetadataRepository = actionMetadataRepository,
    )

    private fun FavoriteRepository.toUseCases() = FavoriteUseCases(
        GetFavoriteUseCase(this), GetFavoritesUseCase(this), DeleteFavoriteUseCase(this),
        UpdateIsFavorite(this), AddFavoriteUseCase(this), GetFavoritesByCityAndColorUseCase(this),
    )
}
