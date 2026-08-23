package com.example.favoriteplaces.feature_favorites.data.backup

import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteBackupImportPlannerTest {
    @Test
    fun emptyDatabaseImportsEveryRecord() {
        val plan = FavoriteBackupImportPlanner.plan(emptyList(), BACKUP)

        assertEquals(BACKUP, plan.toInsert)
        assertTrue(plan.metadataEnrichments.isEmpty())
        assertEquals(0, plan.unchangedCount)
        assertTrue(plan.conflictingIds.isEmpty())
    }

    @Test
    fun identicalRecordsMakeImportIdempotent() {
        val plan = FavoriteBackupImportPlanner.plan(BACKUP, BACKUP)

        assertTrue(plan.toInsert.isEmpty())
        assertTrue(plan.metadataEnrichments.isEmpty())
        assertEquals(BACKUP.size, plan.unchangedCount)
        assertTrue(plan.conflictingIds.isEmpty())
    }

    @Test
    fun differingExistingIdStopsImportPlan() {
        val changed = BACKUP.first().copy(title = "Changed locally")

        val plan = FavoriteBackupImportPlanner.plan(listOf(changed), BACKUP)

        assertEquals(listOf(1), plan.conflictingIds)
        assertTrue(plan.hasConflicts)
    }

    @Test
    fun missingIdIsRejected() {
        assertThrows(InvalidFavoriteBackupException::class.java) {
            FavoriteBackupImportPlanner.plan(emptyList(), listOf(BACKUP.first().copy(id = null)))
        }
    }

    @Test
    fun missingCurrentPhoneCanBeSafelyEnrichedWithoutReplacingOtherFields() {
        val incoming = BACKUP.first().copy(phoneNumber = "303-555-0101")

        val plan = FavoriteBackupImportPlanner.plan(listOf(BACKUP.first()), listOf(incoming))

        assertEquals(listOf(incoming), plan.metadataEnrichments)
        assertTrue(plan.toInsert.isEmpty())
        assertTrue(plan.conflictingIds.isEmpty())
    }

    @Test
    fun olderBackupCannotErasePhoneAndDifferingPhoneConflicts() {
        val current = BACKUP.first().copy(phoneNumber = "303-555-0101")
        val oldPlan = FavoriteBackupImportPlanner.plan(listOf(current), listOf(BACKUP.first()))
        val conflicting = FavoriteBackupImportPlanner.plan(
            listOf(current),
            listOf(BACKUP.first().copy(phoneNumber = "303-555-9999")),
        )

        assertEquals(1, oldPlan.unchangedCount)
        assertTrue(oldPlan.metadataEnrichments.isEmpty())
        assertEquals(listOf(1), conflicting.conflictingIds)
    }

    private companion object {
        val BACKUP = listOf(
            Favorite(1, "one", "One", "1 Main St", "Notes", 4, true, -1, "Denver", 39.0, -104.0),
            Favorite(2, "two", "Two", "2 Main St", null, null, false, -2, "Denver", 39.1, -104.1)
        )
    }
}
