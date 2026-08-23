package com.example.favoriteplaces.logging

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PrivacySafeLogTest {
    @Test
    fun `failure output excludes throwable message and request data`() {
        val secretBearingMessage =
            "https://maps.googleapis.com/place?key=secret-key&input=private-search"

        val output = PrivacySafeLog.formatFailure(
            message = "Place request failed",
            throwable = IOException(secretBearingMessage)
        )

        assertEquals("Place request failed [IOException]", output)
        assertFalse(output.contains(secretBearingMessage))
    }

    @Test
    fun `places diagnostic vocabulary contains no record fields`() {
        val allowedDiagnostic =
            "Place details request failed (Authorization, retryable=false, latency=250_to_999ms)"

        listOf("query", "address", "coordinate", "placeId", "token", "note", "key=")
            .forEach { forbidden ->
                assertFalse(allowedDiagnostic.contains(forbidden, ignoreCase = true))
            }
    }

    @Test
    fun `diagnostic backend failures never escape to app operations`() {
        PrivacySafeLog.info("Test", "Safe diagnostic")
        PrivacySafeLog.error("Test", "Safe failure", IOException("private value"))
    }
}
