package com.example.favoriteplaces.feature_favorites.data.backup

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

object FavoriteBackupImportPlanner {
    fun plan(
        current: List<Favorite>,
        backup: List<Favorite>
    ): FavoriteBackupImportPlan {
        val currentById = current.associateBy { favorite ->
            favorite.id ?: throw InvalidFavoriteBackupException(
                "The current database contains a place without an ID"
            )
        }

        val toInsert = mutableListOf<Favorite>()
        val metadataEnrichments = mutableListOf<Favorite>()
        val unchanged = mutableListOf<Favorite>()
        val conflictingIds = mutableListOf<Int>()

        backup.forEach { backupFavorite ->
            val id = backupFavorite.id ?: throw InvalidFavoriteBackupException(
                "Backup places must have an ID before they can be imported"
            )
            val existing = currentById[id]
            when {
                existing == null -> toInsert += backupFavorite
                existing == backupFavorite -> unchanged += backupFavorite
                existing.hasCompatibleCoreAndMetadata(backupFavorite) -> {
                    val merged = existing.mergeMissingMetadata(backupFavorite)
                    if (merged == existing) unchanged += existing else metadataEnrichments += merged
                }
                else -> conflictingIds += id
            }
        }

        return FavoriteBackupImportPlan(
            toInsert = toInsert,
            metadataEnrichments = metadataEnrichments,
            unchangedCount = unchanged.size,
            conflictingIds = conflictingIds.sorted()
        )
    }
}

data class FavoriteBackupImportPlan(
    val toInsert: List<Favorite>,
    val metadataEnrichments: List<Favorite>,
    val unchangedCount: Int,
    val conflictingIds: List<Int>
) {
    val hasConflicts: Boolean = conflictingIds.isNotEmpty()
}

private fun Favorite.hasCompatibleCoreAndMetadata(backup: Favorite): Boolean =
    copy(phoneNumber = null, googlePrimaryType = null) ==
        backup.copy(phoneNumber = null, googlePrimaryType = null) &&
        metadataValuesAreCompatible(phoneNumber, backup.phoneNumber) &&
        metadataValuesAreCompatible(googlePrimaryType, backup.googlePrimaryType)

private fun metadataValuesAreCompatible(current: String?, backup: String?): Boolean =
    current == null || backup == null || current == backup

private fun Favorite.mergeMissingMetadata(backup: Favorite): Favorite = copy(
    phoneNumber = phoneNumber ?: backup.phoneNumber,
    googlePrimaryType = googlePrimaryType ?: backup.googlePrimaryType,
)

class FavoriteBackupConflictException(conflictingIds: List<Int>) : IllegalStateException(
    "Import stopped because place IDs conflict: ${conflictingIds.joinToString()}"
)
