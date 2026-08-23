package com.example.favoriteplaces.feature_favorites.presentation

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.graphics.toArgb
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDatabase
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toDomain
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toEntity
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.ui.theme.placeAccentColors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LegacyWorkflowUiTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var database: FavoriteDatabase
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun insertFixture() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        database = Room.databaseBuilder(
            context,
            FavoriteDatabase::class.java,
            FavoriteDatabase.DATABASE_NAME
        ).build()
        runBlocking(Dispatchers.IO) {
            database.favoriteDao.getFavoriteById(FIXTURE_ID)?.let {
                database.favoriteDao.deleteFavorite(it)
            }
            database.favoriteDao.getFavoriteById(SECOND_FIXTURE_ID)?.let {
                database.favoriteDao.deleteFavorite(it)
            }
            database.favoriteDao.insertFavorite(FIXTURE.toEntity())
            database.favoriteDao.insertFavorite(SECOND_FIXTURE.toEntity())
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(FIXTURE.title).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun removeFixture() {
        scenario.close()
        runBlocking(Dispatchers.IO) {
            val fixture = database.favoriteDao.getFavoriteById(FIXTURE_ID)
            if (fixture != null) database.favoriteDao.deleteFavorite(fixture)
            val secondFixture = database.favoriteDao.getFavoriteById(SECOND_FIXTURE_ID)
            if (secondFixture != null) database.favoriteDao.deleteFavorite(secondFixture)
        }
        database.close()
    }

    @Test
    fun cardHeartAndDetailsSheetsPreserveNonEditableFields() = runBlocking {
        ensureCardView()
        composeRule.onNodeWithTag("saved_color_header").assertIsDisplayed()
        composeRule.onNodeWithTag("primary_navigation").assertIsDisplayed()
        composeRule.onNodeWithText(FIXTURE.address).assertIsDisplayed()
        composeRule.onAllNodesWithText(FIXTURE.content.orEmpty()).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText(FIXTURE.city).assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Rating 2 out of 5")
            .onFirst()
            .assertIsDisplayed()
        val cardRating = composeRule.onNodeWithTag(
            "favorite_rating_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val cardHeart = composeRule.onNodeWithTag(
            "favorite_heart_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val cardMap = composeRule.onNodeWithTag(
            "favorite_map_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val cardHeartVisual = composeRule.onNodeWithTag(
            "favorite_heart_visual_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val cardMapVisual = composeRule.onNodeWithTag(
            "favorite_map_visual_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val cardText = composeRule.onNodeWithTag(
            "favorite_text_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Card rating should be above Favorite", cardRating.bottom <= cardHeart.top)
        assertTrue("Card Favorite should be above Map", cardHeart.bottom <= cardMap.top)
        assertTrue("Card text should start above rating", cardText.top < cardRating.top)
        assertTrue(
            "Card actions should have equal vertical spacing",
            abs(
                (cardHeartVisual.center.y - cardRating.center.y) -
                    (cardMapVisual.center.y - cardHeartVisual.center.y)
            ) <= 1f,
        )

        composeRule.onNodeWithTag("favorite_heart_$FIXTURE_ID").performClick()
        val heartUpdated = waitForFavorite { favorite -> favorite.isFavorite }
        assertNotNull("Favorite heart update was not persisted", heartUpdated)

        composeRule.onNodeWithTag("favorite_card_$FIXTURE_ID").performClick()
        composeRule.onNodeWithTag("place_details").assertIsDisplayed()
        composeRule.onNodeWithText(FIXTURE.address).assertIsDisplayed()
        composeRule.onNodeWithTag("details_name_type_edit").performClick()
        composeRule.onNodeWithTag("details_edit_name").performTextReplacement("Edited UI place")
        composeRule.onNodeWithTag("details_save_name_type").performClick()
        composeRule.onNodeWithTag("details_save_notes").assertIsNotEnabled()
        composeRule.onNodeWithTag("details_edit_notes").performTextReplacement("Edited UI notes")
        composeRule.onNodeWithTag("details_save_notes").assertIsEnabled()
        composeRule.onNodeWithTag("details_save_notes").performClick()
        composeRule.onNodeWithContentDescription("Set rating to 5").performClick()

        val edited = waitForFavorite { favorite ->
            favorite.title == "Edited UI place" &&
                favorite.content == "Edited UI notes" &&
                favorite.rating == 5 &&
                favorite.isFavorite
        }
        assertNotNull("Edited place was not persisted", edited)
        edited ?: return@runBlocking
        assertEquals(FIXTURE.id, edited.id)
        assertEquals(FIXTURE.placeId, edited.placeId)
        assertEquals(FIXTURE.address, edited.address)
        assertEquals(FIXTURE.city, edited.city)
        assertEquals(FIXTURE.color, edited.color)
        assertEquals(FIXTURE.placeType, edited.placeType)
        assertEquals(FIXTURE.latitude, edited.latitude, 0.0)
        assertEquals(FIXTURE.longitude, edited.longitude, 0.0)
        assertEquals(true, edited.isFavorite)
        assertEquals("Edited UI notes", edited.content)
        assertEquals(5, edited.rating)
    }

    @Test
    fun savedCardDisclosureShowsPhoneAndRestaurantActionsOnlyAfterExpansion() {
        ensureCardView()
        composeRule.onAllNodesWithTag("place_call_$FIXTURE_ID").assertCountEquals(0)
        composeRule.onAllNodesWithTag("place_reserve_$FIXTURE_ID").assertCountEquals(0)
        composeRule.onAllNodesWithTag("place_web_$FIXTURE_ID").assertCountEquals(0)

        composeRule.onNodeWithTag("place_actions_toggle_$FIXTURE_ID").performClick()

        composeRule.onNodeWithTag("place_call_$FIXTURE_ID").assertIsDisplayed()
        composeRule.onNodeWithTag("place_reserve_$FIXTURE_ID").assertIsDisplayed()
        composeRule.onNodeWithTag("place_web_$FIXTURE_ID").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Hide actions for ${FIXTURE.title}")
            .assertIsDisplayed()
        val callBounds = composeRule.onNodeWithTag(
            "place_call_$FIXTURE_ID",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val dividerAndCaretBounds = composeRule.onNodeWithTag(
            "place_actions_toggle_$FIXTURE_ID",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expanded actions should stay above the divider and caret",
            callBounds.bottom <= dividerAndCaretBounds.top,
        )
    }

    @Test
    fun listViewPlaceMapRestoresAfterActivityRecreation() {
        ensureListView()

        composeRule.onNodeWithText(FIXTURE.title).assertIsDisplayed()
        composeRule.onNodeWithText(FIXTURE.address).assertIsDisplayed()
        composeRule.onAllNodesWithText(FIXTURE.content.orEmpty()).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText(FIXTURE.city).assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Show ${FIXTURE.title} on map").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Show ${FIXTURE.title} on map").performClick()
        composeRule.waitUntil(timeoutMillis = MAP_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SINGLE_MAP_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SINGLE_MAP_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(FIXTURE.title).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Directions to ${FIXTURE.title}").assertIsDisplayed()
        composeRule.onAllNodesWithTag("primary_navigation").assertCountEquals(0)

        scenario.recreate()
        composeRule.waitUntil(timeoutMillis = MAP_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SINGLE_MAP_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SINGLE_MAP_TAG).assertIsDisplayed()
    }

    @Test
    fun dataBackupDestinationIsReachableFromSavedScreen() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_app_color_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_card_color_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_data_backup").performClick()

        composeRule.onNodeWithText("Data & backup").assertIsDisplayed()
        composeRule.onNodeWithText("Export backup").assertIsDisplayed()
        composeRule.onNodeWithText("Import backup").assertIsDisplayed()
    }

    @Test
    fun settingsColorPickersExposeTheSeventhSharedChoice() {
        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.onNodeWithTag("settings_app_color_menu").performClick()
        composeRule.onNodeWithTag("settings_app_color_wheel")
            .performScrollToNode(hasTestTag("settings_app_color_option_7"))
        composeRule.onNodeWithTag("settings_app_color_option_7").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_app_color_done").performClick()

        composeRule.onNodeWithTag("settings_card_color_menu").performClick()
        composeRule.onNodeWithTag("settings_card_color_wheel")
            .performScrollToNode(hasTestTag("settings_card_color_option_7"))
        composeRule.onNodeWithTag("settings_card_color_option_7").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_card_color_done").performClick()
    }

    @Test
    fun settingsExposesTheSharedNewPlaceTypeManager() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithTag("settings_nearby_types").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("settings_nearby_type_search")
            .performTextReplacement("ramen")
        composeRule.onNodeWithTag("settings_nearby_type_toggle_ramen_restaurant")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun detailsColorDropdownChangesOnlyColor() = runBlocking {
        ensureCardView()
        composeRule.onNodeWithTag("favorite_card_$FIXTURE_ID").performClick()
        composeRule.onNodeWithTag("details_color_header").assertIsDisplayed()
        composeRule.onNodeWithTag("details_color_footer").assertIsDisplayed()
        composeRule.onNodeWithTag("details_color_menu").performClick()
        composeRule.onNodeWithTag("details_color_option_2").performClick()
        composeRule.onNodeWithTag("details_color_done").performClick()

        val expectedColor = placeAccentColors[1].toArgb()
        val updated = waitForFavorite { it.color == expectedColor }
        assertNotNull("Details color update was not persisted", updated)
        assertEquals(FIXTURE.title, updated?.title)
        assertEquals(FIXTURE.content, updated?.content)
        assertEquals(FIXTURE.rating, updated?.rating)
        assertEquals(FIXTURE.isFavorite, updated?.isFavorite)
    }

    @Test
    fun clearingDetailsRatingUpdatesTheCardToZero() = runBlocking {
        ensureCardView()
        composeRule.onAllNodesWithContentDescription("Rating 2 out of 5")
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_card_$FIXTURE_ID").performClick()
        composeRule.onNodeWithContentDescription(
            "Rating 2 of 5, selected. Double tap to clear"
        ).performClick()

        assertNotNull("Unrated state was not persisted", waitForFavorite { it.rating == null })
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Rating 0 out of 5").assertIsDisplayed()
        Unit
    }

    @Test
    fun groupedSavedRowExposesIndependentFavoriteHeart() = runBlocking {
        ensureListView()
        composeRule.onAllNodesWithContentDescription("Rating 2 out of 5")
            .onFirst()
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag(
            "grouped_place_divider",
            useUnmergedTree = true,
        ).onFirst().assertIsDisplayed()
        val groupedRating = composeRule.onNodeWithTag(
            "grouped_rating_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val groupedHeart = composeRule.onNodeWithTag(
            "grouped_favorite_heart_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val groupedMap = composeRule.onNodeWithTag(
            "grouped_map_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val groupedHeartVisual = composeRule.onNodeWithTag(
            "grouped_heart_visual_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val groupedMapVisual = composeRule.onNodeWithTag(
            "grouped_map_visual_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        val groupedText = composeRule.onNodeWithTag(
            "grouped_text_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Grouped rating should be above Favorite", groupedRating.bottom <= groupedHeart.top)
        assertTrue("Grouped Favorite should be above Map", groupedHeart.bottom <= groupedMap.top)
        assertTrue("Grouped text should start above rating", groupedText.top < groupedRating.top)
        assertTrue(
            "Grouped actions should have equal vertical spacing",
            abs(
                (groupedHeartVisual.center.y - groupedRating.center.y) -
                    (groupedMapVisual.center.y - groupedHeartVisual.center.y)
            ) <= 1f,
        )
        composeRule.onNodeWithTag("grouped_favorite_heart_$FIXTURE_ID").performClick()

        val updated = waitForFavorite(Favorite::isFavorite)
        assertNotNull("Grouped Favorite heart update was not persisted", updated)
        assertEquals(FIXTURE.color, updated?.color)
    }

    @Test
    fun detailsMapUsesStableRecordIdAndRestoresAfterActivityRecreation() {
        ensureCardView()
        composeRule.onNodeWithTag("favorite_card_$FIXTURE_ID").performClick()
        composeRule.onNodeWithText(FIXTURE.address).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show place on map").performClick()

        composeRule.waitUntil(timeoutMillis = MAP_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SINGLE_MAP_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SINGLE_MAP_TAG).assertIsDisplayed()

        scenario.recreate()
        composeRule.waitUntil(timeoutMillis = MAP_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SINGLE_MAP_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SINGLE_MAP_TAG).assertIsDisplayed()
    }

    @Test
    fun unsavedDetailsDraftRestoresAfterActivityRecreationWithoutChangingRoom() = runBlocking {
        ensureCardView()
        composeRule.onNodeWithTag("favorite_card_$FIXTURE_ID").performClick()
        composeRule.onNodeWithTag("details_name_type_edit").performClick()
        composeRule.onNodeWithTag("details_edit_name").performTextReplacement("Unsaved restored draft")
        composeRule.onNodeWithTag("details_edit_type").performTextReplacement("Draft wine bar")

        scenario.recreate()
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Unsaved restored draft").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Unsaved restored draft").assertIsDisplayed()
        composeRule.onNodeWithText("Draft wine bar").assertIsDisplayed()

        val persisted = database.favoriteDao.getFavoriteById(FIXTURE_ID)?.toDomain()
        assertEquals(FIXTURE.title, persisted?.title)
        assertEquals(FIXTURE.content, persisted?.content)
        assertEquals(FIXTURE.placeType, persisted?.placeType)
    }

    @Test
    fun primaryNavigationAndFindQueryRestoreWithoutDuplicatingSaved() {
        composeRule.onNodeWithText("Find").performClick()
        composeRule.onNodeWithTag("find_search").assertIsDisplayed()
        composeRule.onNodeWithTag("find_search").performTextReplacement("restored query")

        scenario.recreate()
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("restored query").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("restored query").assertIsDisplayed()
        composeRule.onNodeWithText("Saved").performClick()
        composeRule.onNodeWithText("Places").assertIsDisplayed()
        composeRule.onNodeWithText(FIXTURE.title).assertIsDisplayed()
    }

    @Test
    fun changeLocationMapUsesFixedGestureSafeDialog() {
        composeRule.onNodeWithText("Find").performClick()
        composeRule.onNodeWithText("Change location").performClick()
        composeRule.onNodeWithText("Choose on map").performClick()

        composeRule.onNodeWithTag("location_map_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("location_picker_map").assertIsDisplayed()
        composeRule.onNodeWithTag("use_map_location").assertIsNotEnabled()
        composeRule.onNodeWithTag("location_picker_map").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("location_map_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("use_map_location").assertIsNotEnabled()

        composeRule.onNodeWithText("Search instead").performClick()
        composeRule.onNodeWithText("Type a city or address").assertIsDisplayed()
    }

    @Test
    fun nearbyDefaultsToOurPlacesAndSavedListMapRestore() {
        composeRule.onNodeWithText("Nearby").performClick()
        composeRule.onNodeWithTag("nearby_source_OurPlaces").assertIsDisplayed()
        composeRule.onNodeWithTag("nearby_source_NewPlaces").assertIsDisplayed()
        composeRule.onNodeWithTag("nearby_location").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag("nearby_saved_card_$FIXTURE_ID").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nearby_saved_card_$FIXTURE_ID").assertIsDisplayed()
        val nearbyCardBounds = composeRule.onNodeWithTag("nearby_saved_card_$FIXTURE_ID")
            .fetchSemanticsNode().boundsInRoot
        val nearbyAccentBounds = composeRule.onNodeWithTag(
            "nearby_saved_accent_layer_$FIXTURE_ID",
            useUnmergedTree = true,
        )
            .fetchSemanticsNode().boundsInRoot
        assertTrue(abs(nearbyCardBounds.height - nearbyAccentBounds.height) <= 1f)
        composeRule.onNodeWithText("Within 25 miles", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Map").performClick()
        composeRule.waitUntil(timeoutMillis = MAP_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag("nearby_saved_map").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nearby_saved_map").assertIsDisplayed()
        composeRule.onNodeWithText("Fit results").assertIsDisplayed()

        scenario.recreate()
        composeRule.waitUntil(timeoutMillis = MAP_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag("nearby_saved_map").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nearby_saved_map").assertIsDisplayed()
        composeRule.onNodeWithTag("nearby_source_OurPlaces").assertIsDisplayed()

        composeRule.onNodeWithText("List").performClick()
        composeRule.onNodeWithTag("nearby_saved_card_$FIXTURE_ID").assertIsDisplayed()
        composeRule.onNodeWithText("Filter").performClick()
        composeRule.onNodeWithText("Saved place filters").assertIsDisplayed()
        composeRule.onNodeWithText("Distance").assertIsDisplayed()
        composeRule.onAllNodesWithText("Any distance").assertCountEquals(0)
        composeRule.onNodeWithText("25").assertIsDisplayed()
        composeRule.onNodeWithText("Favorites only").assertIsDisplayed()
    }

    @Test
    fun newPlacesUsesCenteredTypeWheelAndFocusedGoogleTypeSearch() {
        composeRule.onNodeWithText("Nearby").performClick()
        composeRule.onNodeWithTag("nearby_source_NewPlaces").performClick()
        composeRule.onNodeWithTag("nearby_filter").performClick()

        composeRule.onNodeWithText("New place filters").assertIsDisplayed()
        composeRule.onNodeWithTag("nearby_type_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("nearby_type_add").performClick()
        composeRule.onNodeWithText("Place types").assertIsDisplayed()
        composeRule.onNodeWithTag("nearby_type_search").performTextReplacement("ramen")
        composeRule.onNodeWithTag("nearby_type_toggle_ramen_restaurant").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to filters").performClick()
        composeRule.onNodeWithTag("nearby_type_wheel").assertIsDisplayed()
    }

    private fun ensureCardView() {
        if (composeRule.onAllNodesWithTag("favorite_card_$FIXTURE_ID").fetchSemanticsNodes().isEmpty()) {
            composeRule.onNodeWithText("Filter", substring = true).performClick()
            composeRule.onNodeWithTag("group_by_city_switch").performClick()
            composeRule.onNodeWithText("Apply").performClick()
        }
        composeRule.onNodeWithTag("favorite_card_$FIXTURE_ID").assertIsDisplayed()
    }

    private fun ensureListView() {
        if (composeRule.onAllNodesWithTag("favorite_card_$FIXTURE_ID").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Filter", substring = true).performClick()
            composeRule.onNodeWithTag("group_by_city_switch").performClick()
            composeRule.onNodeWithText("Apply").performClick()
        }
        composeRule.onNodeWithTag("grouped_favorite_heart_$FIXTURE_ID").assertIsDisplayed()
    }

    private suspend fun waitForFavorite(predicate: (Favorite) -> Boolean): Favorite? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            val favorite = database.favoriteDao.getFavoriteById(FIXTURE_ID)?.toDomain()
            if (favorite != null && predicate(favorite)) return favorite
            delay(50)
        }
        return null
    }

    private companion object {
        const val FIXTURE_ID = 900_001
        const val SECOND_FIXTURE_ID = 900_002
        const val UI_TIMEOUT_MILLIS = 5_000L
        const val MAP_TIMEOUT_MILLIS = 15_000L
        const val SINGLE_MAP_TAG = "saved_places_map_1"
        val FIXTURE = Favorite(
            id = FIXTURE_ID,
            placeId = "ui-regression-place-id",
            title = "UI Regression Bistro",
            address = "100 Test Ave, Denver, CO",
            content = "Original UI notes",
            rating = 2,
            isFavorite = false,
            color = -3_173_158,
            city = "Map Test / Zürich",
            latitude = 39.7392,
            longitude = -104.9903,
            placeType = "Restaurant",
            phoneNumber = "303-555-0101",
            googlePrimaryType = "restaurant",
        )
        val SECOND_FIXTURE = FIXTURE.copy(
            id = SECOND_FIXTURE_ID,
            placeId = "ui-regression-place-id-2",
            title = "Second Map Fixture",
            address = "200 Test Ave, Denver, CO",
            latitude = 39.7792,
            longitude = -104.9503,
            placeType = null,
        )
    }
}
