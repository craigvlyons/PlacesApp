package com.example.favoriteplaces.feature_favorites.data.data_source.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDatabaseV1ContractTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FavoriteDatabase::class.java
    )

    @Test
    fun version1SchemaMatchesTheReleasedContract() {
        helper.createDatabase(TEST_DATABASE, 1).use { database ->
            val actualColumns = database.query("PRAGMA table_info(`Favorite`)").use { cursor ->
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
                                primaryKey = cursor.getInt(primaryKeyIndex) == 1
                            )
                        )
                    }
                }
            }

            assertEquals(EXPECTED_COLUMNS, actualColumns)
        }
    }

    @Test
    fun version1FixturePreservesEveryStoredField() {
        helper.createDatabase(TEST_DATABASE, 1).use { database ->
            FIXTURE_ROWS.forEach { row ->
                database.insert(
                    "Favorite",
                    SQLiteDatabase.CONFLICT_ABORT,
                    row.toContentValues()
                )
            }

            val actualRows = database.query(
                """
                SELECT id, placeId, title, address, content, rating, isFavorite,
                       color, city, latitude, longitude
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
                                longitude = cursor.getDouble(10)
                            )
                        )
                    }
                }
            }

            assertEquals(FIXTURE_ROWS, actualRows)
            assertEquals(FIXTURE_ROWS.size, actualRows.size)
            assertEquals(EXPECTED_FIXTURE_SHA256, actualRows.sha256())
        }
    }

    @Test
    fun migration1To2PreservesEveryVersion1ValueAndAddsOnlyNullPlaceType() {
        helper.createDatabase(TEST_DATABASE, 1).use { database ->
            FIXTURE_ROWS.forEach { row ->
                database.insert("Favorite", SQLiteDatabase.CONFLICT_ABORT, row.toContentValues())
            }
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            FavoriteDatabaseMigrations.MIGRATION_1_2,
        ).use { database ->
            val migratedRows = database.query(
                """
                SELECT id, placeId, title, address, content, rating, isFavorite,
                       color, city, latitude, longitude, placeType
                FROM Favorite
                ORDER BY id
                """.trimIndent()
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        assertEquals(null, cursor.stringOrNull(11))
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
                            )
                        )
                    }
                }
            }

            assertEquals(FIXTURE_ROWS, migratedRows)
            assertEquals(EXPECTED_FIXTURE_SHA256, migratedRows.sha256())
        }
    }

    @Test
    fun productionMigrationChainOpensTheEarliestSchema() {
        helper.createDatabase(TEST_DATABASE, 1).use { database ->
            database.insert(
                "Favorite",
                SQLiteDatabase.CONFLICT_ABORT,
                FIXTURE_ROWS.first().toContentValues(),
            )
        }

        val database = buildFavoriteDatabase(
            context = ApplicationProvider.getApplicationContext(),
            name = TEST_DATABASE,
        )

        try {
            database.openHelper.writableDatabase.query(
                "SELECT COUNT(*), COUNT(placeType), COUNT(phoneNumber), COUNT(googlePrimaryType) FROM Favorite"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
            }
        } finally {
            database.close()
        }
    }


    @Test
    fun migration2To3PreservesEveryExistingValueAndAddsOnlyNullPhone() {
        helper.createDatabase(TEST_DATABASE, 2).use { database ->
            database.insert(
                "Favorite",
                SQLiteDatabase.CONFLICT_ABORT,
                FIXTURE_ROWS.first().toContentValues().apply {
                    put("placeType", "Restaurant")
                },
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            FavoriteDatabaseMigrations.MIGRATION_2_3,
        ).use { database ->
            database.query(
                """
                SELECT id, placeId, title, address, content, rating, isFavorite,
                       color, city, latitude, longitude, placeType, phoneNumber
                FROM Favorite
                """.trimIndent()
            ).use { cursor ->
                cursor.moveToFirst()
                val expected = FIXTURE_ROWS.first()
                assertEquals(expected.id, cursor.getInt(0))
                assertEquals(expected.placeId, cursor.stringOrNull(1))
                assertEquals(expected.title, cursor.getString(2))
                assertEquals(expected.address, cursor.getString(3))
                assertEquals(expected.content, cursor.stringOrNull(4))
                assertEquals(expected.rating, cursor.intOrNull(5))
                assertEquals(expected.isFavorite, cursor.getInt(6) == 1)
                assertEquals(expected.color, cursor.getInt(7))
                assertEquals(expected.city, cursor.getString(8))
                assertEquals(expected.latitude, cursor.getDouble(9), 0.0)
                assertEquals(expected.longitude, cursor.getDouble(10), 0.0)
                assertEquals("Restaurant", cursor.getString(11))
                assertEquals(null, cursor.stringOrNull(12))
            }
        }
    }

    @Test
    fun migration3To4PreservesEveryExistingValueAndAddsOnlyNullGoogleType() {
        helper.createDatabase(TEST_DATABASE, 3).use { database ->
            database.insert(
                "Favorite",
                SQLiteDatabase.CONFLICT_ABORT,
                FIXTURE_ROWS.first().toContentValues().apply {
                    put("placeType", "Our dinner pick")
                    put("phoneNumber", "303-555-0101")
                },
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            FavoriteDatabaseMigrations.MIGRATION_3_4,
        ).use { database ->
            database.query(
                "SELECT title, placeType, phoneNumber, googlePrimaryType FROM Favorite"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(FIXTURE_ROWS.first().title, cursor.getString(0))
                assertEquals("Our dinner pick", cursor.getString(1))
                assertEquals("303-555-0101", cursor.getString(2))
                assertEquals(null, cursor.stringOrNull(3))
            }
        }
    }

    private fun android.database.Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private fun android.database.Cursor.intOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    private data class ColumnContract(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val primaryKey: Boolean
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
        val longitude: Double
    ) {
        fun toContentValues() = ContentValues().apply {
            put("id", id)
            if (placeId == null) putNull("placeId") else put("placeId", placeId)
            put("title", title)
            put("address", address)
            if (content == null) putNull("content") else put("content", content)
            if (rating == null) putNull("rating") else put("rating", rating)
            put("isFavorite", isFavorite)
            put("color", color)
            put("city", city)
            put("latitude", latitude)
            put("longitude", longitude)
        }

        fun canonicalValue(): String = listOf(
            id.toString(),
            placeId ?: NULL_MARKER,
            title,
            address,
            content ?: NULL_MARKER,
            rating?.toString() ?: NULL_MARKER,
            if (isFavorite) "1" else "0",
            color.toString(),
            city,
            latitude.toString(),
            longitude.toString()
        ).joinToString(FIELD_SEPARATOR)
    }

    private fun List<FixtureRow>.sha256(): String {
        val canonical = joinToString(RECORD_SEPARATOR) { it.canonicalValue() }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val TEST_DATABASE = "favorite-v1-contract-test"
        const val NULL_MARKER = "<null>"
        const val FIELD_SEPARATOR = "\u001f"
        const val RECORD_SEPARATOR = "\u001e"

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
            ColumnContract("longitude", "REAL", notNull = true, primaryKey = false)
        )

        val FIXTURE_ROWS = listOf(
            FixtureRow(1, "place-1", "Root Down", "1600 W 33rd Ave, Denver, CO", null, null, true, -21615, "Denver", 39.7631, -105.0056),
            FixtureRow(2, "place-2", "The Cherry Cricket", "2641 E 2nd Ave, Denver, CO", "", 0, false, -1577573, "Denver", 39.7194, -104.9561),
            FixtureRow(3, "place-3", "Sushi Den", "1487 S Pearl St, Denver, CO", "Long note with punctuation: tacos, maps & memories!", 5, false, -3173158, "Denver", 39.6897, -104.9806),
            FixtureRow(4, null, "Café Élan ☕", "1 Unicode Way, Montréal, QC", "Crème brûlée — return soon.", 3, true, -8266006, "Montréal", 45.5019, -73.5674),
            FixtureRow(5, "place-5", "Goat Patch Brewing Company", "2727 N Cascade Ave #123, Colorado Springs, CO", "Some of my favorite beer.", 4, true, -749647, "Colorado Springs", 38.8722, -104.8225)
        )

        // Update only when the fixture is deliberately reviewed. A changed value must
        // never be accepted merely to make a migration test pass.
        const val EXPECTED_FIXTURE_SHA256 = "2726927dc77562ea50b42d2f8013036b8b6f384120fcfba9cd11758b48f40c0f"
    }
}
