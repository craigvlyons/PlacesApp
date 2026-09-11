package com.example.favoriteplaces.feature_favorites.presentation.settings

import com.example.favoriteplaces.testutil.FakeSavedPlacesPreferencesRepository
import com.example.favoriteplaces.feature_favorites.domain.use_case.settings.UpdateNearbyTypePickerUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `app and new-place colors persist independently`() = runTest(dispatcher) {
        val repository = FakeSavedPlacesPreferencesRepository()
        val viewModel = SettingsViewModel(repository, UpdateNearbyTypePickerUseCase(repository))
        advanceUntilIdle()

        viewModel.setAppColor(DARK_BLUE)
        advanceUntilIdle()
        assertEquals(DARK_BLUE, viewModel.preferences.value.appColor)
        assertEquals(LIGHT_BLUE, viewModel.preferences.value.defaultCardColor)

        viewModel.setDefaultCardColor(DARK_BLUE)
        advanceUntilIdle()
        assertEquals(DARK_BLUE, viewModel.preferences.value.appColor)
        assertEquals(DARK_BLUE, viewModel.preferences.value.defaultCardColor)
    }

    @Test
    fun `nearby picker types can be added and removed from settings`() = runTest(dispatcher) {
        val repository = FakeSavedPlacesPreferencesRepository()
        val viewModel = SettingsViewModel(repository, UpdateNearbyTypePickerUseCase(repository))
        advanceUntilIdle()

        viewModel.addNearbyType("ramen_restaurant")
        advanceUntilIdle()
        assertEquals(
            listOf("restaurant", "coffee_shop", "bar", "bakery", "ramen_restaurant"),
            viewModel.preferences.value.nearbyDiscoveryTypeIds,
        )

        viewModel.removeNearbyType("coffee_shop")
        advanceUntilIdle()
        assertEquals(
            listOf("restaurant", "bar", "bakery", "ramen_restaurant"),
            viewModel.preferences.value.nearbyDiscoveryTypeIds,
        )
    }

    private companion object {
        const val LIGHT_BLUE = -8_266_006
        const val DARK_BLUE = -12_816_751
    }
}
