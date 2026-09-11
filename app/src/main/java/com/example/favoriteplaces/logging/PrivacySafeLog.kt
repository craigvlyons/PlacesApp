package com.example.favoriteplaces.logging

import android.util.Log

/** Logs a useful failure category without serializing exception messages or request data. */
object PrivacySafeLog {
    fun info(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: RuntimeException) {
            // Diagnostics must never interrupt the user-visible operation being diagnosed.
        }
    }

    fun error(tag: String, message: String, throwable: Throwable) {
        try {
            Log.e(tag, formatFailure(message, throwable))
        } catch (_: RuntimeException) {
            // Diagnostics must never interrupt the user-visible operation being diagnosed.
        }
    }

    internal fun formatFailure(message: String, throwable: Throwable): String {
        val failureType = throwable.javaClass.simpleName.ifBlank { "Throwable" }
        return "$message [$failureType]"
    }
}
