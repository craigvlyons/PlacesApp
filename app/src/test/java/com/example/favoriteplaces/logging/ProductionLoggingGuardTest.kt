package com.example.favoriteplaces.logging

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionLoggingGuardTest {
    @Test
    fun `production diagnostics use the privacy safe logger`() {
        val sourceRoot = File("src/main/java")
        val approvedLogger = "com/example/favoriteplaces/logging/PrivacySafeLog.kt"
        val forbidden = listOf(
            "android.util.Log",
            "Log.d(",
            "Log.i(",
            "Log.w(",
            "Log.e(",
            "printStackTrace(",
            "println(",
            "System.out",
            "System.err",
        )
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .filterNot { it.relativeTo(sourceRoot).invariantSeparatorsPath == approvedLogger }
            .filter { file -> forbidden.any(file.readText()::contains) }
            .map { it.relativeTo(sourceRoot).invariantSeparatorsPath }
            .toList()

        assertTrue("Production logging bypasses PrivacySafeLog: $offenders", offenders.isEmpty())
    }
}
