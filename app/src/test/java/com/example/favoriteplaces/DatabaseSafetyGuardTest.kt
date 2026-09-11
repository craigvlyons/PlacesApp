package com.example.favoriteplaces

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSafetyGuardTest {

    @Test
    fun productionCodeDoesNotEnableDestructiveRoomFallback() {
        val sourceRoot = File("src/main")
        assertTrue("Expected Android production sources at ${sourceRoot.absolutePath}", sourceRoot.isDirectory)

        val destructiveCalls = listOf(
            "fallbackToDestructiveMigration(",
            "fallbackToDestructiveMigrationFrom(",
            "fallbackToDestructiveMigrationOnDowngrade(",
            "clearAllTables(",
        )
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .filter { source -> destructiveCalls.any(source.readText()::contains) }
            .map { it.relativeTo(sourceRoot).path }
            .toList()

        assertFalse(
            "Destructive Room fallback would put the household database at risk: $offenders",
            offenders.isNotEmpty()
        )
    }
}
