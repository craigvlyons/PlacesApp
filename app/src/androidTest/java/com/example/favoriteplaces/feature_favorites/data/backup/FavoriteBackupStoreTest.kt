package com.example.favoriteplaces.feature_favorites.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDatabase
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toDomain
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toEntity
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteBackupStoreTest {
    @Test
    fun fileExportAndTransactionalImportAreVerifiedAndIdempotent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
        val destination = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
        val backupFile = File(context.cacheDir, "store-round-trip.json")
        val backupUri = Uri.fromFile(backupFile)

        try {
            source.favoriteDao.insertFavoritesForRestore(FIXTURE.map { it.toEntity() })
            val exportResult = FavoriteBackupStore(context, source).exportTo(backupUri)
            val destinationStore = FavoriteBackupStore(context, destination)
            val verified = destinationStore.inspect(backupUri)

            val firstImport = destinationStore.import(verified)
            val secondImport = destinationStore.import(verified)

            assertEquals(FIXTURE.size, exportResult.recordCount)
            assertEquals(FIXTURE.size, firstImport.insertedCount)
            assertEquals(0, firstImport.unchangedCount)
            assertEquals(0, secondImport.insertedCount)
            assertEquals(FIXTURE.size, secondImport.unchangedCount)
            assertEquals(FIXTURE, destination.favoriteDao.getFavoritesSnapshot().map { it.toDomain() })
        } finally {
            backupFile.delete()
            source.close()
            destination.close()
        }
    }

    @Test
    fun conflictLeavesDestinationUnchanged() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
        val destination = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
        val backupFile = File(context.cacheDir, "store-conflict.json")
        val backupUri = Uri.fromFile(backupFile)

        try {
            source.favoriteDao.insertFavoritesForRestore(FIXTURE.map { it.toEntity() })
            FavoriteBackupStore(context, source).exportTo(backupUri)
            val conflicting = FIXTURE.first().copy(title = "Newer local title")
            destination.favoriteDao.insertFavorite(conflicting.toEntity())
            val destinationStore = FavoriteBackupStore(context, destination)
            val verified = destinationStore.inspect(backupUri)

            try {
                destinationStore.import(verified)
                fail("Expected a conflicting import to stop")
            } catch (_: FavoriteBackupConflictException) {
                // Expected: the transaction must leave the existing database unchanged.
            }
            assertEquals(
                listOf(conflicting),
                destination.favoriteDao.getFavoritesSnapshot().map { it.toDomain() }
            )
        } finally {
            backupFile.delete()
            source.close()
            destination.close()
        }
    }

    @Test
    fun importFillsOnlyMissingGoogleMetadataAndIsThenIdempotent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val destination = Room.inMemoryDatabaseBuilder(context, FavoriteDatabase::class.java).build()
        try {
            destination.favoriteDao.insertFavoritesForRestore(FIXTURE.map { it.toEntity() })
            val incoming = FIXTURE.mapIndexed { index, favorite ->
                if (index == 0) favorite.copy(
                    phoneNumber = "303-555-0101",
                    googlePrimaryType = "restaurant",
                ) else favorite
            }
            val verified = FavoriteBackupCodec.decodeAndVerify(FavoriteBackupCodec.encode(incoming))
            val store = FavoriteBackupStore(context, destination)

            val first = store.import(verified)
            val second = store.import(verified)

            assertEquals(1, first.enrichedCount)
            assertEquals(0, first.insertedCount)
            assertEquals(0, second.enrichedCount)
            assertEquals(incoming, destination.favoriteDao.getFavoritesSnapshot().map { it.toDomain() })
        } finally {
            destination.close()
        }
    }

    private companion object {
        val FIXTURE = listOf(
            Favorite(1, "one", "One", "1 Main St", "Notes", 4, true, -1, "Denver", 39.0, -104.0),
            Favorite(2, "two", "Two", "2 Main St", null, null, false, -2, "Denver", 39.1, -104.1)
        )
    }
}
