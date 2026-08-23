package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PlaceActionDisclosure(
    placeId: Int?,
    placeName: String,
    canCall: Boolean,
    canReserve: Boolean,
    expanded: Boolean,
    loading: Boolean,
    showDivider: Boolean,
    onToggle: () -> Unit,
    onCall: () -> Unit,
    onReserve: () -> Unit,
    onWeb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showDivider) return

    androidx.compose.foundation.layout.Column(modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            when {
                loading -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "Loading call details…",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 2.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val webWeight = when {
                        canCall && canReserve -> 0.27f
                        canCall -> 0.52f
                        canReserve -> 0.38f
                        else -> 1f
                    }
                    if (canCall) {
                        OutlinedButton(
                            onClick = onCall,
                            modifier = Modifier
                                .weight(if (canReserve) 0.25f else 0.48f)
                                .testTag("place_call_$placeId"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, Modifier.size(18.dp))
                            Text("Call", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    if (canReserve) {
                        Button(
                            onClick = onReserve,
                            modifier = Modifier
                                .weight(if (canCall) 0.48f else 0.62f)
                                .testTag("place_reserve_$placeId"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Icon(Icons.Default.EventSeat, contentDescription = null, Modifier.size(18.dp))
                            Text("Reservation", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    OutlinedButton(
                        onClick = onWeb,
                        modifier = Modifier
                            .weight(webWeight)
                            .testTag("place_web_$placeId"),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, Modifier.size(18.dp))
                        Text("Web", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = if (expanded) {
                        "Hide actions for $placeName"
                    } else {
                        "Show actions for $placeName"
                    }
                }
                .testTag("place_actions_toggle_$placeId"),
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp)
                    .testTag("grouped_place_divider"),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
