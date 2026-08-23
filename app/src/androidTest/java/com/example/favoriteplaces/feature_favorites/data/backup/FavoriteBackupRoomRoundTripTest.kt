package com.example.favoriteplaces.feature_favorites.data.backup

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDatabase
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toDomain
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toEntity
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteBackupRoomRoundTripTest {

    @Test
    fun exportAndDisposableRestorePreserveEveryRoomValue() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
        val restored = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()

        try {
            FIXTURE.forEach { source.favoriteDao.insertFavorite(it.toEntity()) }
            val sourceSnapshot = source.favoriteDao.getFavoritesSnapshot()
            val exported = FavoriteBackupCodec.encode(sourceSnapshot.map { it.toDomain() })
            val verified = FavoriteBackupCodec.decodeAndVerify(exported)

            restored.withTransaction {
                check(restored.favoriteDao.getFavoritesSnapshot().isEmpty())
                verified.favorites.forEach { restored.favoriteDao.insertFavorite(it.toEntity()) }
            }

            val restoredSnapshot = restored.favoriteDao.getFavoritesSnapshot()
            val restoredExport = FavoriteBackupCodec.encode(restoredSnapshot.map { it.toDomain() })
            val restoredVerified = FavoriteBackupCodec.decodeAndVerify(restoredExport)

            assertEquals(sourceSnapshot, restoredSnapshot)
            assertEquals(FIXTURE.size, restoredSnapshot.size)
            assertEquals(EXPECTED_DIGEST, verified.recordsSha256)
            assertEquals(verified.recordsSha256, restoredVerified.recordsSha256)
            assertEquals(exported, restoredExport)
        } finally {
            source.close()
            restored.close()
        }
    }

    private companion object {
        const val EXPECTED_DIGEST = "79dc8f722287a996d2c019dd1531467006698f98507c8df022a9f949229b4987"

        val FIXTURE = listOf(
            Favorite(1, "place-1", "Root Down", "1600 W 33rd Ave, Denver, CO", null, null, true, -21615, "Denver", 39.7631, -105.0056, "Restaurant"),
            Favorite(2, "place-2", "The Cherry Cricket", "2641 E 2nd Ave, Denver, CO", "", 0, false, -1577573, "Denver", 39.7194, -104.9561),
            Favorite(3, "place-3", "Sushi Den", "1487 S Pearl St, Denver, CO", "Long note with punctuation: tacos, maps & memories!", 5, false, -3173158, "Denver", 39.6897, -104.9806),
            Favorite(4, null, "Café Élan ☕", "1 Unicode Way, Montréal, QC", "Crème brûlée — return soon.", 3, true, -8266006, "Montréal", 45.5019, -73.5674),
            Favorite(5, "place-5", "Goat Patch Brewing Company", "2727 N Cascade Ave #123, Colorado Springs, CO", "Some of my favorite beer.", 4, true, -749647, "Colorado Springs", 38.8722, -104.8225, "Brewery")
        )
    }
}
