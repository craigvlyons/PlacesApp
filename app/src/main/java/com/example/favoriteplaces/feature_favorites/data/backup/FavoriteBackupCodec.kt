package com.example.favoriteplaces.feature_favorites.data.backup

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object FavoriteBackupCodec {
    const val FORMAT = "places-app-backup"
    const val VERSION = 4
    const val LEGACY_VERSION = 1
    const val PLACE_TYPE_VERSION = 2
    const val PHONE_VERSION = 3

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .create()

    fun encode(favorites: List<Favorite>): String {
        val records = favorites
            .map(BackupFavorite::fromFavorite)
            .sortedWith(compareBy({ it.id ?: Int.MIN_VALUE }, { it.canonicalValue(VERSION) }))
        return gson.toJson(
            FavoriteBackupEnvelope(
                format = FORMAT,
                version = VERSION,
                recordCount = records.size,
                recordsSha256 = records.sha256(VERSION),
                records = records
            )
        )
    }

    fun decodeAndVerify(json: String): VerifiedFavoriteBackup {
        val root = try {
            JsonParser.parseString(json).asJsonObject
        } catch (exception: Exception) {
            throw InvalidFavoriteBackupException("Backup is not valid JSON", exception)
        }

        requireExactFields(root, ENVELOPE_FIELDS, "backup", VERSION)
        val format = root.requiredString("format")
        if (format != FORMAT) {
            throw InvalidFavoriteBackupException("Unsupported backup format")
        }

        val version = root.requiredInt("version")
        if (version !in SUPPORTED_VERSIONS) {
            throw InvalidFavoriteBackupException("Unsupported backup version: $version")
        }

        val declaredCount = root.requiredInt("recordCount")
        if (declaredCount < 0) {
            throw InvalidFavoriteBackupException("Backup record count cannot be negative")
        }

        val declaredDigest = root.requiredString("recordsSha256")
        val recordsJson = root.get("records")
        if (recordsJson == null || !recordsJson.isJsonArray) {
            throw InvalidFavoriteBackupException("Backup records must be an array")
        }

        val records = recordsJson.asJsonArray.mapIndexed { index, element ->
            if (!element.isJsonObject) {
                throw InvalidFavoriteBackupException("Record $index must be an object")
            }
            val recordJson = element.asJsonObject
            val expectedFields = when (version) {
                LEGACY_VERSION -> RECORD_FIELDS_V1
                PLACE_TYPE_VERSION -> RECORD_FIELDS_V2
                PHONE_VERSION -> RECORD_FIELDS_V3
                else -> RECORD_FIELDS_V4
            }
            requireExactFields(recordJson, expectedFields, "record $index", version)
            parseRecord(recordJson, index, version)
        }

        if (records.size != declaredCount) {
            throw InvalidFavoriteBackupException(
                "Backup record count mismatch: declared $declaredCount, found ${records.size}"
            )
        }
        val actualDigest = records.sha256(version)
        if (!actualDigest.equals(declaredDigest, ignoreCase = true)) {
            throw InvalidFavoriteBackupException("Backup content digest does not match")
        }
        val duplicateIds = records.mapNotNull { it.id }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateIds.isNotEmpty()) {
            throw InvalidFavoriteBackupException("Backup contains duplicate place IDs")
        }

        return VerifiedFavoriteBackup(
            version = version,
            recordCount = records.size,
            recordsSha256 = actualDigest,
            favorites = records.map(BackupFavorite::toFavorite)
        )
    }

    private fun parseRecord(record: JsonObject, index: Int, version: Int): BackupFavorite {
        val latitude = record.requiredDouble("latitude", index)
        val longitude = record.requiredDouble("longitude", index)
        if (!latitude.isFinite() || !longitude.isFinite()) {
            throw InvalidFavoriteBackupException("Record $index has non-finite coordinates")
        }
        return BackupFavorite(
            id = record.requiredNullableInt("id", index),
            placeId = record.requiredNullableString("placeId", index),
            title = record.requiredString("title", index),
            address = record.requiredString("address", index),
            content = record.requiredNullableString("content", index),
            rating = record.requiredNullableInt("rating", index),
            isFavorite = record.requiredBoolean("isFavorite", index),
            color = record.requiredInt("color", index),
            city = record.requiredString("city", index),
            latitude = latitude,
            longitude = longitude,
            placeType = if (version >= PLACE_TYPE_VERSION) {
                record.requiredNullableString("placeType", index)
            } else {
                null
            },
            phoneNumber = if (version >= PHONE_VERSION) {
                record.requiredNullableString("phoneNumber", index)
            } else {
                null
            },
            googlePrimaryType = if (version >= VERSION) {
                record.requiredNullableString("googlePrimaryType", index)
            } else {
                null
            },
        )
    }

    private fun requireExactFields(
        value: JsonObject,
        expected: Set<String>,
        label: String,
        version: Int,
    ) {
        if (value.keySet() != expected) {
            throw InvalidFavoriteBackupException(
                "$label fields do not match backup version $version"
            )
        }
    }

    private fun JsonObject.requiredString(name: String, recordIndex: Int? = null): String {
        val value = get(name)
        if (value == null || value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "must be a string"))
        }
        return value.asString
    }

    private fun JsonObject.requiredNullableString(name: String, recordIndex: Int): String? {
        val value = get(name)
        if (value == null) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "is missing"))
        }
        if (value.isJsonNull) return null
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "must be a string or null"))
        }
        return value.asString
    }

    private fun JsonObject.requiredInt(name: String, recordIndex: Int? = null): Int {
        val value = get(name)
        if (value == null || value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "must be an integer"))
        }
        return try {
            val integer = value.asBigDecimal.toBigIntegerExact()
            if (integer < INT_MIN || integer > INT_MAX) {
                throw ArithmeticException("Integer is outside the 32-bit range")
            }
            integer.toInt()
        } catch (exception: ArithmeticException) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "must be an integer"), exception)
        }
    }

    private fun JsonObject.requiredNullableInt(name: String, recordIndex: Int): Int? {
        val value = get(name)
        if (value == null) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "is missing"))
        }
        if (value.isJsonNull) return null
        return requiredInt(name, recordIndex)
    }

    private fun JsonObject.requiredBoolean(name: String, recordIndex: Int): Boolean {
        val value = get(name)
        if (value == null || value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "must be a boolean"))
        }
        return value.asBoolean
    }

    private fun JsonObject.requiredDouble(name: String, recordIndex: Int): Double {
        val value = get(name)
        if (value == null || value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "must be a number"))
        }
        return try {
            value.asDouble
        } catch (exception: NumberFormatException) {
            throw InvalidFavoriteBackupException(fieldError(name, recordIndex, "must be a number"), exception)
        }
    }

    private fun fieldError(name: String, recordIndex: Int?, detail: String): String =
        if (recordIndex == null) "Field $name $detail" else "Record $recordIndex field $name $detail"

    private fun List<BackupFavorite>.sha256(version: Int): String {
        val digestInput = buildString {
            this@sha256.forEach { record ->
                appendLengthPrefixed(record.canonicalValue(version))
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun BackupFavorite.canonicalValue(version: Int): String = buildString {
        appendLengthPrefixed(id?.toString())
        appendLengthPrefixed(placeId)
        appendLengthPrefixed(title)
        appendLengthPrefixed(address)
        appendLengthPrefixed(content)
        appendLengthPrefixed(rating?.toString())
        appendLengthPrefixed(if (isFavorite) "1" else "0")
        appendLengthPrefixed(color.toString())
        appendLengthPrefixed(city)
        appendLengthPrefixed(latitude.toString())
        appendLengthPrefixed(longitude.toString())
        if (version >= PLACE_TYPE_VERSION) appendLengthPrefixed(placeType)
        if (version >= PHONE_VERSION) appendLengthPrefixed(phoneNumber)
        if (version >= VERSION) appendLengthPrefixed(googlePrimaryType)
    }

    private fun StringBuilder.appendLengthPrefixed(value: String?) {
        if (value == null) {
            append("-1:")
            return
        }
        append(value.toByteArray(StandardCharsets.UTF_8).size)
        append(':')
        append(value)
    }

    private val ENVELOPE_FIELDS = setOf(
        "format",
        "version",
        "recordCount",
        "recordsSha256",
        "records"
    )
    private val RECORD_FIELDS_V1 = setOf(
        "id",
        "placeId",
        "title",
        "address",
        "content",
        "rating",
        "isFavorite",
        "color",
        "city",
        "latitude",
        "longitude"
    )
    private val RECORD_FIELDS_V2 = RECORD_FIELDS_V1 + "placeType"
    private val RECORD_FIELDS_V3 = RECORD_FIELDS_V2 + "phoneNumber"
    private val RECORD_FIELDS_V4 = RECORD_FIELDS_V3 + "googlePrimaryType"
    private val SUPPORTED_VERSIONS = setOf(
        LEGACY_VERSION,
        PLACE_TYPE_VERSION,
        PHONE_VERSION,
        VERSION,
    )
    private val INT_MIN = BigInteger.valueOf(Int.MIN_VALUE.toLong())
    private val INT_MAX = BigInteger.valueOf(Int.MAX_VALUE.toLong())
}

data class VerifiedFavoriteBackup(
    val version: Int,
    val recordCount: Int,
    val recordsSha256: String,
    val favorites: List<Favorite>
)

class InvalidFavoriteBackupException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

private data class FavoriteBackupEnvelope(
    val format: String,
    val version: Int,
    val recordCount: Int,
    val recordsSha256: String,
    val records: List<BackupFavorite>
)

private data class BackupFavorite(
    val id: Int?,
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
    fun toFavorite() = Favorite(
        id = id,
        placeId = placeId,
        title = title,
        address = address,
        content = content,
        rating = rating,
        isFavorite = isFavorite,
        color = color,
        city = city,
        latitude = latitude,
        longitude = longitude,
        placeType = placeType,
        phoneNumber = phoneNumber,
        googlePrimaryType = googlePrimaryType,
    )

    companion object {
        fun fromFavorite(favorite: Favorite) = BackupFavorite(
            id = favorite.id,
            placeId = favorite.placeId,
            title = favorite.title,
            address = favorite.address,
            content = favorite.content,
            rating = favorite.rating,
            isFavorite = favorite.isFavorite,
            color = favorite.color,
            city = favorite.city,
            latitude = favorite.latitude,
            longitude = favorite.longitude,
            placeType = favorite.placeType,
            phoneNumber = favorite.phoneNumber,
            googlePrimaryType = favorite.googlePrimaryType,
        )
    }
}
