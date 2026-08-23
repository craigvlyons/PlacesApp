package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

data class WordWheelOption(
    val key: String,
    val label: String,
)

@Composable
fun WordWheelPicker(
    options: List<WordWheelOption>,
    selectedKey: String,
    testTagPrefix: String,
    onSelected: (String) -> Unit,
) {
    require(options.isNotEmpty()) { "The word wheel requires at least one choice." }
    require(options.map(WordWheelOption::key).distinct().size == options.size) {
        "The word wheel requires unique option keys."
    }
    val optionSignature = options.joinToString(separator = "\u0000", transform = WordWheelOption::key)
    val selectedIndex = options.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val loops = options.size >= VISIBLE_ITEM_COUNT
    val pickerCenter = if (loops) options.size * 100 else 0
    val pickerItemCount = if (loops) pickerCenter * 2 + options.size else 1
    var focusedPickerItem by remember(optionSignature) {
        mutableIntStateOf(pickerCenter + selectedIndex)
    }
    val pickerState = rememberLazyListState()
    val pickerScope = rememberCoroutineScope()
    val currentOnSelected by rememberUpdatedState(onSelected)

    LaunchedEffect(optionSignature, selectedKey) {
        val target = pickerCenter + selectedIndex
        focusedPickerItem = target
        pickerState.scrollToItem(target)
    }

    LaunchedEffect(pickerState, optionSignature) {
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
                currentOnSelected(options[focusedItem % options.size].key)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WHEEL_HEIGHT)
            .clip(RoundedCornerShape(20.dp))
            .testTag("${testTagPrefix}_wheel"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ) {}
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(WHEEL_HEIGHT),
            state = pickerState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = pickerState),
            contentPadding = PaddingValues(vertical = ITEM_HEIGHT * CENTER_PADDING_ITEMS),
        ) {
            items(pickerItemCount) { pickerItem ->
                val option = options[pickerItem % options.size]
                val isFocused = pickerItem == focusedPickerItem
                val distance = abs(pickerItem - focusedPickerItem)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT)
                        .graphicsLayer {
                            alpha = when (distance) {
                                0 -> 1f
                                1 -> 0.72f
                                2 -> 0.48f
                                else -> 0.28f
                            }
                            val scale = when (distance) {
                                0 -> 1f
                                1 -> 0.94f
                                else -> 0.88f
                            }
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(role = Role.RadioButton) {
                            pickerScope.launch { pickerState.animateScrollToItem(pickerItem) }
                        }
                        .semantics {
                            role = Role.RadioButton
                            selected = isFocused
                            contentDescription = buildString {
                                append(option.label)
                                append(", ")
                                append((pickerItem % options.size) + 1)
                                append(" of ")
                                append(options.size)
                            }
                        }
                        .testTag("${testTagPrefix}_option_${option.key}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val ITEM_HEIGHT = 44.dp
private const val VISIBLE_ITEM_COUNT = 5
private const val CENTER_PADDING_ITEMS = VISIBLE_ITEM_COUNT / 2
private val WHEEL_HEIGHT = ITEM_HEIGHT * VISIBLE_ITEM_COUNT
