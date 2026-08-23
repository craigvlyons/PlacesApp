package com.example.favoriteplaces.feature_favorites.presentation.citymap

import androidx.lifecycle.SavedStateHandle
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.AddFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.DeleteFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesByCityAndColorUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.UpdateIsFavorite
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CityMapViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun groupArgumentsRestoreExactRoomSubsetIncludingUnicodeAndNegativeColor() = runTest(dispatcher) {
        val repository = FakeFavoriteRepository(listOf(DENVER, OTHER_COLOR, OTHER_CITY))
        val viewModel = CityMapViewModel(
            favoriteUseCases = repository.toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screen.CityMapScreen.CITY to "Café / Denver",
                    Screen.CityMapScreen.COLOR to -749647,
                )
            ),
        )

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Café / Denver", viewModel.state.value.title)
        assertEquals(listOf(DENVER), viewModel.state.value.places)
    }

    @Test
    fun favoriteIdRestoresSingleRecord() = runTest(dispatcher) {
        val viewModel = CityMapViewModel(
            favoriteUseCases = FakeFavoriteRepository(listOf(DENVER)).toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(Screen.CityMapScreen.FAVORITE_ID to DENVER.id)
            ),
        )

        advanceUntilIdle()

        assertEquals(DENVER.title, viewModel.state.value.title)
        assertEquals(listOf(DENVER), viewModel.state.value.places)
    }

    @Test
    fun missingRecordAndInvalidCoordinatesBecomeExplicitEmptyStates() = runTest(dispatcher) {
        val missing = CityMapViewModel(
            favoriteUseCases = FakeFavoriteRepository(emptyList()).toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(Screen.CityMapScreen.FAVORITE_ID to 404)
            ),
        )
        val invalid = CityMapViewModel(
            favoriteUseCases = FakeFavoriteRepository(listOf(INVALID)).toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(Screen.CityMapScreen.FAVORITE_ID to INVALID.id)
            ),
        )

        advanceUntilIdle()

        assertEquals("This saved place is no longer available.", missing.state.value.message)
        assertEquals(emptyList<Favorite>(), invalid.state.value.places)
        assertEquals(
            "These saved places do not have valid map coordinates.",
            invalid.state.value.message,
        )
    }

    @Test
    fun incompleteArgumentsDoNotQueryOrCrash() = runTest(dispatcher) {
        val viewModel = CityMapViewModel(
            favoriteUseCases = FakeFavoriteRepository(listOf(DENVER)).toUseCases(),
            savedStateHandle = SavedStateHandle(),
        )

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("This map link is incomplete.", viewModel.state.value.message)
    }

    @Test
    fun validRecordsRemainVisibleWhenAnotherRecordHasInvalidCoordinates() = runTest(dispatcher) {
        val viewModel = CityMapViewModel(
            favoriteUseCases = FakeFavoriteRepository(listOf(DENVER, INVALID)).toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screen.CityMapScreen.CITY to DENVER.city,
                    Screen.CityMapScreen.COLOR to DENVER.color,
                )
            ),
        )

        advanceUntilIdle()

        assertEquals(listOf(DENVER), viewModel.state.value.places)
        assertEquals(
            "Some saved places could not be shown because their coordinates are invalid.",
            viewModel.state.value.coordinateWarning,
        )
    }

    @Test
    fun roomReadFailuresBecomeStableUserVisibleStates() = runTest(dispatcher) {
        val single = CityMapViewModel(
            favoriteUseCases = FakeFavoriteRepository(emptyList(), failSingle = true).toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(Screen.CityMapScreen.FAVORITE_ID to 1)
            ),
        )
        val group = CityMapViewModel(
            favoriteUseCases = FakeFavoriteRepository(emptyList(), failGroup = true).toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screen.CityMapScreen.CITY to "Denver",
                    Screen.CityMapScreen.COLOR to -1,
                )
            ),
        )

        advanceUntilIdle()

        assertEquals("The saved place could not be loaded.", single.state.value.message)
        assertEquals("Saved places could not be loaded.", group.state.value.message)
    }

    @Test
    fun mapFavoriteHeartUpdatesOnlyTheSelectedRecord() = runTest(dispatcher) {
        val repository = FakeFavoriteRepository(listOf(DENVER, OTHER_COLOR))
        val viewModel = CityMapViewModel(
            favoriteUseCases = repository.toUseCases(),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screen.CityMapScreen.CITY to DENVER.city,
                    Screen.CityMapScreen.COLOR to DENVER.color,
                )
            ),
        )
        advanceUntilIdle()

        viewModel.updateFavoriteHeart(requireNotNull(DENVER.id), true)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.places.single().isFavorite)
        assertEquals(true, repository.saved.single { it.id == DENVER.id }.isFavorite)
        assertEquals(OTHER_COLOR, repository.saved.single { it.id == OTHER_COLOR.id })
    }

    private class FakeFavoriteRepository(
        initial: List<Favorite>,
        private val failSingle: Boolean = false,
        private val failGroup: Boolean = false,
    ) : FavoriteRepository {
        private val favorites = MutableStateFlow(initial)
        val saved: List<Favorite> get() = favorites.value

        override fun getFavorites(): Flow<List<Favorite>> = favorites
        override suspend fun getFavoriteById(id: Int): Favorite? {
            if (failSingle) error("fixture single read failure")
            return favorites.value.firstOrNull { it.id == id }
        }

        override suspend fun insertFavorite(favorite: Favorite) = Unit
        override suspend fun updateIsFavorite(id: Int, isFavorite: Boolean) {
            favorites.value = favorites.value.map {
                if (it.id == id) it.copy(isFavorite = isFavorite) else it
            }
        }
        override suspend fun deleteFavorite(favorite: Favorite) = Unit
        override fun getFavoritesByCityAndColor(city: String, color: Int): Flow<List<Favorite>> =
            if (failGroup) {
                flow { error("fixture group read failure") }
            } else {
                MutableStateFlow(favorites.value.filter { it.city == city && it.color == color })
            }
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
        val DENVER = Favorite(
            id = 1,
            placeId = "p1",
            title = "Café Élan",
            address = "Address",
            content = null,
            rating = 5,
            color = -749647,
            city = "Café / Denver",
            latitude = 39.75,
            longitude = -104.99,
        )
        val OTHER_COLOR = DENVER.copy(id = 2, color = -1)
        val OTHER_CITY = DENVER.copy(id = 3, city = "Boulder")
        val INVALID = DENVER.copy(id = 4, latitude = Double.NaN)
    }
}
