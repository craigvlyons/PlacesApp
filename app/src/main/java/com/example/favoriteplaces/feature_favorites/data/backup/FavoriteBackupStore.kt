package com.example.favoriteplaces.feature_favorites.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDatabase
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toDomain
import com.example.favoriteplaces.feature_favorites.data.data_source.db.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class FavoriteBackupStore @Inject constructor(
    @ApplicationContext context: Context,
    private val database: FavoriteDatabase
) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun exportTo(uri: Uri): BackupExportResult = withContext(Dispatchers.IO) {
        val json = database.withTransaction {
            FavoriteBackupCodec.encode(
                database.favoriteDao.getFavoritesSnapshot().map { it.toDomain() }
            )
        }
        val verified = FavoriteBackupCodec.decodeAndVerify(json)
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(StandardCharsets.UTF_8)
            ?.use { writer ->
            writer.write(json)
        } ?: throw IOException("The selected backup file could not be opened for writing")
        BackupExportResult(
            recordCount = verified.recordCount,
            recordsSha256 = verified.recordsSha256
        )
    }

    suspend fun inspect(uri: Uri): VerifiedFavoriteBackup = withContext(Dispatchers.IO) {
        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("The selected backup file could not be opened")
        val bytes = input.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BACKUP_BYTES) {
                    throw InvalidFavoriteBackupException("Backup is larger than 10 MB")
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
        FavoriteBackupCodec.decodeAndVerify(String(bytes, StandardCharsets.UTF_8))
    }

    suspend fun import(backup: VerifiedFavoriteBackup): BackupImportResult =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val plan = FavoriteBackupImportPlanner.plan(
                    current = database.favoriteDao.getFavoritesSnapshot().map { it.toDomain() },
                    backup = backup.favorites
                )
                if (plan.hasConflicts) {
                    throw FavoriteBackupConflictException(plan.conflictingIds)
                }
                if (plan.toInsert.isNotEmpty()) {
                    database.favoriteDao.insertFavoritesForRestore(
                        plan.toInsert.map { it.toEntity() }
                    )
                }
                plan.metadataEnrichments.forEach { favorite ->
                    val id = requireNotNull(favorite.id)
                    if (database.favoriteDao.fillMissingGoogleMetadata(
                            id = id,
                            phoneNumber = favorite.phoneNumber,
                            googlePrimaryType = favorite.googlePrimaryType,
                        ) != 1
                    ) {
                        throw FavoriteBackupConflictException(listOf(id))
                    }
                }
                BackupImportResult(
                    insertedCount = plan.toInsert.size,
                    enrichedCount = plan.metadataEnrichments.size,
                    unchangedCount = plan.unchangedCount,
                    recordsSha256 = backup.recordsSha256
                )
            }
        }

    private companion object {
        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    }
}

data class BackupExportResult(
    val recordCount: Int,
    val recordsSha256: String
)

data class BackupImportResult(
    val insertedCount: Int,
    val enrichedCount: Int,
    val unchangedCount: Int,
    val recordsSha256: String
)
