package com.example.favoriteplaces.feature_favorites.data.data_source.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoBehaviorTest {
    private lateinit var database: FavoriteDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun createDisposableDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
        dao = database.favoriteDao
    }

    @After
    fun closeDisposableDatabase() {
        database.close()
    }

    @Test
    fun insertUpdateDeleteAndUndoPreserveTheCompleteRecord() = runBlocking {
        dao.insertFavorite(ROOT_DOWN.toEntity())
        assertEquals(ROOT_DOWN, requireNotNull(dao.getFavoriteById(ROOT_DOWN_ID)).toDomain())

        val edited = ROOT_DOWN.copy(title = "Root Down updated", content = "Keep every other field")
        dao.updateNameAndType(ROOT_DOWN_ID, edited.title, edited.placeType)
        dao.updateNotes(ROOT_DOWN_ID, edited.content)
        assertEquals(edited, requireNotNull(dao.getFavoriteById(ROOT_DOWN_ID)).toDomain())

        dao.updateIsFavorite(ROOT_DOWN_ID, false)
        val heartChanged = requireNotNull(dao.getFavoriteById(ROOT_DOWN_ID)).toDomain()
        assertEquals(edited.copy(isFavorite = false), heartChanged)
        assertEquals(edited.color, heartChanged.color)

        dao.deleteFavorite(heartChanged.toEntity())
        assertTrue(dao.getFavoritesSnapshot().isEmpty())

        dao.insertFavorite(heartChanged.toEntity())
        assertEquals(listOf(heartChanged), dao.getFavoritesSnapshot().map { it.toDomain() })
    }

    @Test
    fun conflictingPrimaryKeyIsRejectedWithoutOverwritingTheExistingPlace() = runBlocking {
        dao.insertFavorite(ROOT_DOWN.toEntity())

        var rejected = false
        try {
            dao.insertFavorite(ROOT_DOWN.copy(title = "Unexpected replacement").toEntity())
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(ROOT_DOWN, requireNotNull(dao.getFavoriteById(ROOT_DOWN_ID)).toDomain())
    }

    @Test
    fun applicationInsertRejectsANewDuplicateGooglePlaceIdAtomically() = runBlocking {
        assertTrue(dao.insertFavoriteIfPlaceIdMissing(ROOT_DOWN.toEntity()))

        val inserted = dao.insertFavoriteIfPlaceIdMissing(
            ROOT_DOWN.copy(id = 2, title = "Duplicate Google place").toEntity()
        )

        assertFalse(inserted)
        assertEquals(listOf(ROOT_DOWN), dao.getFavoritesSnapshot().map { it.toDomain() })
    }

    @Test
    fun duplicatePlaceIdsRemainAllowedByTheReleasedSchema() = runBlocking {
        val duplicate = ROOT_DOWN.copy(id = 2, title = "Duplicate legacy row")
        dao.insertFavorite(ROOT_DOWN.toEntity())
        dao.insertFavorite(duplicate.toEntity())

        assertTrue(dao.isFavoriteExists(ROOT_DOWN_PLACE_ID))
        assertEquals(listOf(ROOT_DOWN, duplicate), dao.getFavoritesSnapshot().map { it.toDomain() })
    }

    @Test
    fun cityAndColorSubsetMatchesTheReleasedListAndMapGrouping() = runBlocking {
        val sameGroup = ROOT_DOWN.copy(id = 2, title = "Same group")
        val otherColor = ROOT_DOWN.copy(id = 3, title = "Other color", color = -1577573)
        val otherCity = ROOT_DOWN.copy(id = 4, title = "Other city", city = "Boulder")
        listOf(ROOT_DOWN, sameGroup, otherColor, otherCity).forEach { dao.insertFavorite(it.toEntity()) }

        val subset = dao.getFavoritesByCityAndColor(ROOT_DOWN.city, ROOT_DOWN.color).first()

        assertEquals(listOf(ROOT_DOWN, sameGroup), subset.map { it.toDomain() })
        assertFalse(subset.any { it.id == otherColor.id || it.id == otherCity.id })
    }

    @Test
    fun detailsFieldUpdatesNeverOverwriteUnrelatedValues() = runBlocking {
        val original = ROOT_DOWN.copy(placeType = "Restaurant")
        dao.insertFavorite(original.toEntity())

        assertEquals(1, dao.updateNameAndType(ROOT_DOWN_ID, "Updated name", "Cocktail bar"))
        assertEquals(1, dao.updateNotes(ROOT_DOWN_ID, "Updated notes\nSecond line"))
        assertEquals(1, dao.updateColor(ROOT_DOWN_ID, -9))
        assertEquals(1, dao.updateRating(ROOT_DOWN_ID, null))

        val updated = requireNotNull(dao.getFavoriteById(ROOT_DOWN_ID)).toDomain()
        assertEquals(
            original.copy(
                title = "Updated name",
                placeType = "Cocktail bar",
                content = "Updated notes\nSecond line",
                color = -9,
                rating = null,
            ),
            updated,
        )
        assertEquals(0, dao.updateColor(404, -1))
    }

    private companion object {
        const val ROOT_DOWN_ID = 1
        const val ROOT_DOWN_PLACE_ID = "place-1"
        val ROOT_DOWN = Favorite(
            id = ROOT_DOWN_ID,
            placeId = ROOT_DOWN_PLACE_ID,
            title = "Root Down",
            address = "1600 W 33rd Ave, Denver, CO",
            content = "Legacy note",
            rating = 5,
            isFavorite = true,
            color = -21615,
            city = "Denver",
            latitude = 39.7631,
            longitude = -105.0056
        )
    }
}
