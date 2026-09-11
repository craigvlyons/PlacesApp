package com.example.favoriteplaces.feature_favorites.data.backup

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import com.google.gson.JsonObject

class LegacyMigrationGoldenTest {
    @Test
    fun offlineMigrationToolMatchesTheApplicationBackupCodec() {
        val capture = File("../tools/legacy_migration/fixtures/complete-capture.json").readText()
        val recordsJson = com.google.gson.JsonParser.parseString(capture)
            .asJsonObject.getAsJsonArray("records")
        val backupRecords = recordsJson.map { element ->
            element.asJsonObject.apply {
                remove("evidence")
                remove("provenance")
            }
        }
        val verified = FavoriteBackupCodec.decodeAndVerify(
            envelope(1, EXPECTED_DIGEST, backupRecords).toString()
        )

        assertEquals(2, verified.recordCount)
        assertEquals(EXPECTED_DIGEST, verified.recordsSha256)
    }

    @Test
    fun versionTwoOfflineMigrationToolMatchesTheApplicationBackupCodec() {
        val capture = File("../tools/legacy_migration/fixtures/complete-capture-v2.json").readText()
        val recordsJson = com.google.gson.JsonParser.parseString(capture)
            .asJsonObject.getAsJsonArray("records")
        val backupRecords = recordsJson.map { element ->
            element.asJsonObject.apply {
                remove("evidence")
                remove("provenance")
            }
        }
        val verified = FavoriteBackupCodec.decodeAndVerify(
            envelope(2, VERSION_TWO_EXPECTED_DIGEST, backupRecords).toString()
        )

        assertEquals(2, verified.version)
        assertEquals(VERSION_TWO_EXPECTED_DIGEST, verified.recordsSha256)
        assertEquals("Restaurant", verified.favorites.first().placeType)
        assertEquals(null, verified.favorites.first().phoneNumber)
    }

    private fun envelope(
        version: Int,
        digest: String,
        records: List<JsonObject>,
    ) = JsonObject().apply {
        addProperty("format", FavoriteBackupCodec.FORMAT)
        addProperty("version", version)
        addProperty("recordCount", records.size)
        addProperty("recordsSha256", digest)
        add("records", com.google.gson.JsonArray().apply { records.forEach(::add) })
    }

    private companion object {
        const val EXPECTED_DIGEST = "24abac7d58c12bc018d0df4528f557aacaecc3fde570e1d0a5ae3086fc1fcf25"
        const val VERSION_TWO_EXPECTED_DIGEST =
            "b73f3e0e5f52a0034357514582a11c1c2b08782dbb172d20c10487529085a52a"
    }
}
