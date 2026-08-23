package com.example.favoriteplaces.feature_favorites.presentation.favorites

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.example.favoriteplaces.logging.PrivacySafeLog
import java.net.URLEncoder

fun openPlaceDialer(context: Context, phoneNumber: String): Boolean {
    val uri = dialUriString(phoneNumber)?.toUri() ?: return false
    return try {
        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
        true
    } catch (exception: ActivityNotFoundException) {
        PrivacySafeLog.error(TAG, "No dialer is installed", exception)
        false
    } catch (exception: SecurityException) {
        PrivacySafeLog.error(TAG, "Dialer launch was blocked", exception)
        false
    }
}

fun openOpenTableSearch(context: Context, placeName: String): ReservationLaunchResult {
    val normalizedName = placeName.trim()
    if (normalizedName.isEmpty()) return ReservationLaunchResult(opened = false, copied = false)

    val copied = try {
        context.getSystemService<ClipboardManager>()?.let { clipboard ->
            clipboard.setPrimaryClip(ClipData.newPlainText("Restaurant name", normalizedName))
            true
        } ?: false
    } catch (exception: RuntimeException) {
        PrivacySafeLog.error(TAG, "Restaurant-name clipboard copy failed", exception)
        false
    }

    val opened = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, openTableSearchUrl(normalizedName).toUri()))
        true
    } catch (exception: ActivityNotFoundException) {
        PrivacySafeLog.error(TAG, "No OpenTable link handler is installed", exception)
        false
    } catch (exception: SecurityException) {
        PrivacySafeLog.error(TAG, "OpenTable launch was blocked", exception)
        false
    }
    return ReservationLaunchResult(opened = opened, copied = copied)
}

fun openGooglePlaceSearch(context: Context, placeName: String, address: String): Boolean {
    val url = googlePlaceSearchUrl(placeName, address) ?: return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        true
    } catch (exception: ActivityNotFoundException) {
        PrivacySafeLog.error(TAG, "No web link handler is installed", exception)
        false
    } catch (exception: SecurityException) {
        PrivacySafeLog.error(TAG, "Google place search launch was blocked", exception)
        false
    }
}

internal fun dialUriString(phoneNumber: String): String? {
    val normalized = buildString {
        phoneNumber.trim().forEachIndexed { index, character ->
            if (character.isDigit() || (character == '+' && index == 0)) append(character)
        }
    }
    if (normalized.count(Char::isDigit) < MIN_PHONE_DIGITS) return null
    return "tel:$normalized"
}

internal fun openTableSearchUrl(placeName: String): String {
    val encodedName = URLEncoder.encode(placeName.trim(), "UTF-8")
        .replace("+", "%20")
    return "https://www.opentable.com/s?covers=2&term=$encodedName"
}

internal fun googlePlaceSearchUrl(placeName: String, address: String): String? {
    val query = listOf(placeName.trim(), address.trim())
        .filter(String::isNotEmpty)
        .joinToString(" ")
    if (query.isEmpty()) return null
    val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
    return "https://www.google.com/search?q=$encodedQuery"
}

data class ReservationLaunchResult(
    val opened: Boolean,
    val copied: Boolean,
)

private const val MIN_PHONE_DIGITS = 3
private const val TAG = "PlaceActionLauncher"
