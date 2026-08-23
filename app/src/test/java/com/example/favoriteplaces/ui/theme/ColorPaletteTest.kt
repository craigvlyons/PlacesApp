package com.example.favoriteplaces.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPaletteTest {
    @Test
    fun darkerPurpleIsAppendedWithoutReorderingExistingChoices() {
        val originalColors = listOf(
            RedOrange,
            LightGreen,
            Violet,
            BabyBlue,
            DarkerBlue,
            RedPink,
        )

        assertEquals(originalColors, placeAccentColors.take(originalColors.size))
        assertEquals(0xFF76519A.toInt(), placeAccentColors.last().toArgb())
        assertTrue(DarkerPurple.luminance() < Violet.luminance())
    }

    @Test
    fun appChromeContentUsesContrastAppropriateForLightAndDarkColors() {
        assertEquals(Color(0xFF202126), readableContentColor(BabyBlue))
        assertEquals(Color.White, readableContentColor(DarkerPurple))
    }
}
