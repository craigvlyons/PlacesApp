package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.data.models.Prediction
import com.google.android.gms.maps.model.LatLng


@Composable
fun PredictionCard(
    prediction: Prediction,
    location: LatLng,
    selectedId: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
) {
    Box(
        modifier = modifier
            .shadow(6.dp, shape = RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.primary)
            .border(0.5.dp, Color.DarkGray, shape = RoundedCornerShape(cornerRadius))

    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val clipPath = Path().apply {
                lineTo(size.width , 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            clipPath(clipPath) {
                drawRoundRect(
                    color = Color.Transparent,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Title
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = prediction.structuredFormatting.mainText,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Address
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = prediction.structuredFormatting.secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val joinedString = prediction.types.joinToString(" | ")
                    Text(
                        text = joinedString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 3
                    )
                }

            // Show map if selected
            if (selectedId == prediction.placeId) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .padding(5.dp),
                    text = "Location: $location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

}



