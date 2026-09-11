package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun ThirdPartyAttributions(attributions: List<String>) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    AndroidView(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                textSize = 12f
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.text = HtmlCompat.fromHtml(
                attributions.joinToString("<br>"),
                HtmlCompat.FROM_HTML_MODE_COMPACT,
            )
        },
    )
}
