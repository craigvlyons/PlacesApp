package com.example.favoriteplaces.feature_favorites.data.backup

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteBackupCodecTest {

    @Test
    fun exportIsDeterministicAndPreservesEveryStoredField() {
        val firstExport = FavoriteBackupCodec.encode(FIXTURE.reversed())
        val secondExport = FavoriteBackupCodec.encode(FIXTURE)

        assertEquals(firstExport, secondExport)
        val root = JsonParser.parseString(firstExport).asJsonObject
        assertEquals(FavoriteBackupCodec.FORMAT, root.get("format").asString)
        assertEquals(FavoriteBackupCodec.VERSION, root.get("version").asInt)
        assertEquals(FIXTURE.size, root.get("recordCount").asInt)
        assertEquals(EXPECTED_DIGEST, root.get("recordsSha256").asString)
        assertTrue(firstExport.contains("\"placeId\":null"))
        assertTrue(firstExport.contains("\"content\":null"))
        assertTrue(firstExport.contains("\"rating\":null"))
        assertTrue(firstExport.contains("\"placeType\":\"Restaurant\""))
        assertTrue(firstExport.contains("\"phoneNumber\":\"(303) 555-0101\""))
        assertTrue(firstExport.contains("\"googlePrimaryType\":\"restaurant\""))

        val verified = FavoriteBackupCodec.decodeAndVerify(firstExport)
        assertEquals(FIXTURE, verified.favorites)
        assertEquals(FIXTURE.size, verified.recordCount)
        assertEquals(EXPECTED_DIGEST, verified.recordsSha256)
    }

    @Test
    fun importRejectsContentChangedWithoutUpdatingDigest() {
        val exported = FavoriteBackupCodec.encode(FIXTURE)
        val tampered = exported.replace("Root Down", "Root Down changed")

        assertThrows(InvalidFavoriteBackupException::class.java) {
            FavoriteBackupCodec.decodeAndVerify(tampered)
        }
    }

    @Test
    fun importRejectsUnknownVersionsAndMissingFields() {
        val exported = FavoriteBackupCodec.encode(FIXTURE)
        val futureVersion = exported.replace("\"version\":4", "\"version\":5")
        val missingPlaceId = exported.replaceFirst("\"placeId\":\"place-1\",", "")

        assertThrows(InvalidFavoriteBackupException::class.java) {
            FavoriteBackupCodec.decodeAndVerify(futureVersion)
        }
        assertThrows(InvalidFavoriteBackupException::class.java) {
            FavoriteBackupCodec.decodeAndVerify(missingPlaceId)
        }
    }

    @Test
    fun importRequiresTheExactRecordShapeForEachSupportedVersion() {
        val version4 = FavoriteBackupCodec.encode(FIXTURE)
        val version4MissingGoogleType = version4.replaceFirst(",\"googlePrimaryType\":null", "")

        assertThrows(InvalidFavoriteBackupException::class.java) {
            FavoriteBackupCodec.decodeAndVerify(version4MissingGoogleType)
        }

        val version1WithUnexpectedType = FavoriteBackupCodec.encode(FIXTURE.map { it.copy(placeType = null) })
            .replace("\"version\":4", "\"version\":1")
            .replace(Regex(",\"phoneNumber\":(?:null|\"[^\"]*\")"), "")
            .replace(Regex(",\"googlePrimaryType\":(?:null|\"[^\"]*\")"), "")
            .replace(
                Regex("\"recordsSha256\":\"[0-9a-f]+\""),
                "\"recordsSha256\":\"$LEGACY_DIGEST\"",
            )

        assertThrows(InvalidFavoriteBackupException::class.java) {
            FavoriteBackupCodec.decodeAndVerify(version1WithUnexpectedType)
        }
    }

    @Test
    fun version1BackupRemainsReadableAndMapsMissingTypeToNull() {
        val legacyFixture = FIXTURE.map { it.copy(placeType = null, phoneNumber = null, googlePrimaryType = null) }
        val legacyJson = FavoriteBackupCodec.encode(legacyFixture)
            .replace("\"version\":4", "\"version\":1")
            .replace(",\"placeType\":null", "")
            .replace(",\"phoneNumber\":null", "")
            .replace(",\"googlePrimaryType\":null", "")
            .replace(
                Regex("\"recordsSha256\":\"[0-9a-f]+\""),
                "\"recordsSha256\":\"$LEGACY_DIGEST\"",
            )

        val verified = FavoriteBackupCodec.decodeAndVerify(legacyJson)

        assertEquals(FavoriteBackupCodec.LEGACY_VERSION, verified.version)
        assertEquals(legacyFixture, verified.favorites)
        assertTrue(verified.favorites.all { it.placeType == null })
        assertEquals(LEGACY_DIGEST, verified.recordsSha256)
    }

    @Test
    fun version3BackupRemainsReadableAndMapsMissingGoogleTypeToNull() {
        val version3Fixture = FIXTURE.map { it.copy(googlePrimaryType = null) }
        val version3Json = FavoriteBackupCodec.encode(version3Fixture)
            .replace("\"version\":4", "\"version\":3")
            .replace(",\"googlePrimaryType\":null", "")
            .replace(
                Regex("\"recordsSha256\":\"[0-9a-f]+\""),
                "\"recordsSha256\":\"$VERSION_THREE_DIGEST\"",
            )

        val verified = FavoriteBackupCodec.decodeAndVerify(version3Json)

        assertEquals(FavoriteBackupCodec.PHONE_VERSION, verified.version)
        assertEquals(version3Fixture, verified.favorites)
    }

    private companion object {
        const val EXPECTED_DIGEST = "9f013f0543028cf817d585e5e2c5d77bec45a31b391320aead6479184a4c8aa8"
        const val LEGACY_DIGEST = "2334a8715a4302b4b57eef47578eb2757713cda73fae166ce0a6d2f401cd4bfa"
        const val VERSION_THREE_DIGEST = "77fde1063a814b07a40a88f4c356ae92afbee19d9c58f88f8c39218fb111c553"

        val FIXTURE = listOf(
            Favorite(1, "place-1", "Root Down", "1600 W 33rd Ave, Denver, CO", null, null, true, -21615, "Denver", 39.7631, -105.0056, "Restaurant", "(303) 555-0101", "restaurant"),
            Favorite(2, "place-2", "The Cherry Cricket", "2641 E 2nd Ave, Denver, CO", "", 0, false, -1577573, "Denver", 39.7194, -104.9561),
            Favorite(3, "place-3", "Sushi Den", "1487 S Pearl St, Denver, CO", "Long note with punctuation: tacos, maps & memories!", 5, false, -3173158, "Denver", 39.6897, -104.9806),
            Favorite(4, null, "Café Élan ☕", "1 Unicode Way, Montréal, QC", "Crème brûlée — return soon.", 3, true, -8266006, "Montréal", 45.5019, -73.5674),
            Favorite(5, "place-5", "Goat Patch Brewing Company", "2727 N Cascade Ave #123, Colorado Springs, CO", "Some of my favorite beer.", 4, true, -749647, "Colorado Springs", 38.8722, -104.8225, "Brewery")
        )
    }
}
