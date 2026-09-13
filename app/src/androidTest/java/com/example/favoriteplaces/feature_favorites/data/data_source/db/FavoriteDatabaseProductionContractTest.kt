package com.example.favoriteplaces.feature_favorites.data.data_source.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDatabaseProductionContractTest {
    private lateinit var database: FavoriteDatabase

    @Before
    fun createProductionBaseline() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
    }

    @After
    fun closeProductionBaseline() {
        database.close()
    }

    @Test
    fun schemaContainsEveryProductionField() {
        val actualColumns = database.openHelper.readableDatabase
            .query("PRAGMA table_info(`Favorite`)")
            .use { cursor ->
                buildList {
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    val typeIndex = cursor.getColumnIndexOrThrow("type")
                    val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
                    val primaryKeyIndex = cursor.getColumnIndexOrThrow("pk")
                    while (cursor.moveToNext()) {
                        add(
                            ColumnContract(
                                name = cursor.getString(nameIndex),
                                type = cursor.getString(typeIndex),
                                notNull = cursor.getInt(notNullIndex) == 1,
                                primaryKey = cursor.getInt(primaryKeyIndex) == 1,
                            )
                        )
                    }
                }
            }

        assertEquals(EXPECTED_COLUMNS, actualColumns)
    }

    @Test
    fun fixturePreservesEveryProductionField() {
        FIXTURE_ROWS.forEach { row ->
            database.openHelper.writableDatabase.insert(
                "Favorite",
                SQLiteDatabase.CONFLICT_ABORT,
                row.toContentValues(),
            )
        }

        val actualRows = database.openHelper.readableDatabase.query(
            """
            SELECT id, placeId, title, address, content, rating, isFavorite,
                   color, city, latitude, longitude, placeType, phoneNumber,
                   googlePrimaryType
            FROM Favorite
            ORDER BY id
            """.trimIndent()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        FixtureRow(
                            id = cursor.getInt(0),
                            placeId = cursor.stringOrNull(1),
                            title = cursor.getString(2),
                            address = cursor.getString(3),
                            content = cursor.stringOrNull(4),
                            rating = cursor.intOrNull(5),
                            isFavorite = cursor.getInt(6) == 1,
                            color = cursor.getInt(7),
                            city = cursor.getString(8),
                            latitude = cursor.getDouble(9),
                            longitude = cursor.getDouble(10),
                            placeType = cursor.stringOrNull(11),
                            phoneNumber = cursor.stringOrNull(12),
                            googlePrimaryType = cursor.stringOrNull(13),
                        )
                    )
                }
            }
        }

        assertEquals(FIXTURE_ROWS, actualRows)
    }

    private fun android.database.Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private fun android.database.Cursor.intOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    private data class ColumnContract(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val primaryKey: Boolean,
    )

    private data class FixtureRow(
        val id: Int,
        val placeId: String?,
        val title: String,
        val address: String,
        val content: String?,
        val rating: Int?,
        val isFavorite: Boolean,
        val color: Int,
        val city: String,
        val latitude: Double,
        val longitude: Double,
        val placeType: String?,
        val phoneNumber: String?,
        val googlePrimaryType: String?,
    ) {
        fun toContentValues() = ContentValues().apply {
            put("id", id)
            putNullable("placeId", placeId)
            put("title", title)
            put("address", address)
            putNullable("content", content)
            if (rating == null) putNull("rating") else put("rating", rating)
            put("isFavorite", isFavorite)
            put("color", color)
            put("city", city)
            put("latitude", latitude)
            put("longitude", longitude)
            putNullable("placeType", placeType)
            putNullable("phoneNumber", phoneNumber)
            putNullable("googlePrimaryType", googlePrimaryType)
        }

        private fun ContentValues.putNullable(key: String, value: String?) {
            if (value == null) putNull(key) else put(key, value)
        }
    }

    private companion object {
        val EXPECTED_COLUMNS = listOf(
            ColumnContract("id", "INTEGER", notNull = false, primaryKey = true),
            ColumnContract("placeId", "TEXT", notNull = false, primaryKey = false),
            ColumnContract("title", "TEXT", notNull = true, primaryKey = false),
            ColumnContract("address", "TEXT", notNull = true, primaryKey = false),
            ColumnContract("content", "TEXT", notNull = false, primaryKey = false),
            ColumnContract("rating", "INTEGER", notNull = false, primaryKey = false),
            ColumnContract("isFavorite", "INTEGER", notNull = true, primaryKey = false),
            ColumnContract("color", "INTEGER", notNull = true, primaryKey = false),
            ColumnContract("city", "TEXT", notNull = true, primaryKey = false),
            ColumnContract("latitude", "REAL", notNull = true, primaryKey = false),
            ColumnContract("longitude", "REAL", notNull = true, primaryKey = false),
            ColumnContract("placeType", "TEXT", notNull = false, primaryKey = false),
            ColumnContract("phoneNumber", "TEXT", notNull = false, primaryKey = false),
            ColumnContract("googlePrimaryType", "TEXT", notNull = false, primaryKey = false),
        )

        val FIXTURE_ROWS = listOf(
            FixtureRow(
                id = 1,
                placeId = "place-1",
                title = "Root Down",
                address = "1600 W 33rd Ave, Denver, CO",
                content = "Return for dinner.",
                rating = 5,
                isFavorite = true,
                color = -21615,
                city = "Denver",
                latitude = 39.7631,
                longitude = -105.0056,
                placeType = "Restaurant",
                phoneNumber = "303-555-0101",
                googlePrimaryType = "restaurant",
            ),
            FixtureRow(
                id = 2,
                placeId = null,
                title = "Café Élan ☕",
                address = "1 Unicode Way, Montréal, QC",
                content = "Crème brûlée — return soon.",
                rating = null,
                isFavorite = false,
                color = -8266006,
                city = "Montréal",
                latitude = 45.5019,
                longitude = -73.5674,
                placeType = null,
                phoneNumber = null,
                googlePrimaryType = null,
            ),
        )
    }
}
