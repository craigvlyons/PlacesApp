package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.ui.theme.placeAccentColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ColorWheelDropdown(
    selectedColor: Int,
    enabled: Boolean = true,
    colors: List<Color> = placeAccentColors,
    testTagPrefix: String,
    onColorSelected: (Int) -> Unit,
) {
    require(colors.isNotEmpty()) { "The color wheel requires at least one choice." }
    val pickerCenter = colors.size * 100
    val pickerItemCount = pickerCenter * 2 + colors.size
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = colors.indexOfFirst { it.toArgb() == selectedColor }
    var pendingIndex by remember(selectedColor) { mutableIntStateOf(selectedIndex.coerceAtLeast(0)) }
    var focusedPickerItem by remember { mutableIntStateOf(pickerCenter + pendingIndex) }
    val pickerState = rememberLazyListState(initialFirstVisibleItemIndex = focusedPickerItem)
    val pickerScope = rememberCoroutineScope()

    LaunchedEffect(expanded, selectedIndex) {
        if (expanded) {
            pendingIndex = selectedIndex.coerceAtLeast(0)
            focusedPickerItem = pickerCenter + pendingIndex
            pickerState.scrollToItem(focusedPickerItem)
        }
    }

    LaunchedEffect(pickerState, colors.size) {
        snapshotFlow { pickerState.layoutInfo }
            .map { layoutInfo ->
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    abs(item.offset + item.size / 2 - viewportCenter)
                }?.index
            }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { focusedItem ->
                focusedPickerItem = focusedItem
                pendingIndex = focusedItem % colors.size
            }
    }

    Box {
        Row(
            modifier = Modifier
                .sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, role = Role.Button) { expanded = true }
                .semantics {
                    contentDescription = if (selectedIndex >= 0) {
                        "Color ${selectedIndex + 1} of ${colors.size}. Change color"
                    } else {
                        "Current custom color. Change color"
                    }
                    stateDescription = "Selected"
                }
                .testTag("${testTagPrefix}_menu")
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerColorCircle(color = Color(selectedColor))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(76.dp),
            offset = DpOffset(x = (-10).dp, y = (-116).dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(168.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.width(68.dp).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ) {}
                LazyColumn(
                    modifier = Modifier
                        .width(68.dp)
                        .height(168.dp)
                        .testTag("${testTagPrefix}_wheel"),
                    state = pickerState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = pickerState),
                    contentPadding = PaddingValues(vertical = 56.dp),
                ) {
                    items(pickerItemCount) { pickerItem ->
                        val colorIndex = pickerItem % colors.size
                        val color = colors[colorIndex]
                        val isFocused = pickerItem == focusedPickerItem
                        val distance = abs(pickerItem - focusedPickerItem)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .graphicsLayer {
                                    alpha = when (distance) {
                                        0 -> 1f
                                        1 -> 0.68f
                                        else -> 0.4f
                                    }
                                    val scale = if (isFocused) 1f else 0.86f
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable(role = Role.RadioButton) {
                                    focusedPickerItem = pickerItem
                                    pendingIndex = colorIndex
                                    pickerScope.launch { pickerState.animateScrollToItem(pickerItem) }
                                }
                                .semantics {
                                    selected = isFocused
                                    stateDescription = if (isFocused) "Selected" else "Not selected"
                                    contentDescription = "Color ${colorIndex + 1} of ${colors.size}"
                                }
                                .testTag("${testTagPrefix}_option_${colorIndex + 1}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            PickerColorCircle(color = color)
                        }
                    }
                }
            }
            TextButton(
                modifier = Modifier.fillMaxWidth().testTag("${testTagPrefix}_done"),
                onClick = {
                    expanded = false
                    val chosenColor = colors[pendingIndex].toArgb()
                    if (chosenColor != selectedColor) onColorSelected(chosenColor)
                },
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun PickerColorCircle(color: Color) {
    Box(
        modifier = Modifier.size(40.dp).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    CircleShape,
                )
        )
    }
}
