package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.places.GoogleNearbyPlaceTypeCatalog
import com.example.favoriteplaces.feature_favorites.domain.model.places.NearbyCategory

@Composable
fun DiscoveryTypeManager(
    configuredCategories: List<NearbyCategory>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDone: () -> Unit,
    testTagPrefix: String = "nearby_type",
    backContentDescription: String = "Back to filters",
) {
    var query by rememberSaveable { mutableStateOf("") }
    val configuredIds = configuredCategories.mapTo(mutableSetOf(), NearbyCategory::storageId)
    val matches = remember(query) { GoogleNearbyPlaceTypeCatalog.search(query, limit = 40) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription,
                )
            }
            Text(
                "Place types",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
            )
            TextButton(onClick = onDone) { Text("Done") }
        }
        Text(
            "In your picker",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        if (configuredCategories.isEmpty()) {
            Text(
                "Only Any type is shown. Add types below.",
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(configuredCategories, key = NearbyCategory::storageId) { option ->
                    OutlinedButton(
                        onClick = { onRemove(option.storageId) },
                        contentPadding = PaddingValues(start = 14.dp, end = 8.dp),
                    ) {
                        Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.Close, contentDescription = "Remove ${option.label}")
                    }
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(MAX_QUERY_LENGTH) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .testTag("${testTagPrefix}_search"),
            placeholder = { Text("Search Google place types") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear type search")
                    }
                }
            } else null,
            singleLine = true,
        )
        Text(
            if (query.isBlank()) "Common additions" else "Matching Google types",
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        HorizontalDivider()
        if (matches.isEmpty()) {
            Text(
                "No supported Google place type matches that search.",
                modifier = Modifier.padding(vertical = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(matches, key = NearbyCategory::storageId) { option ->
                    val isAdded = option.storageId in configuredIds
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option.label, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                if (isAdded) onRemove(option.storageId) else onAdd(option.storageId)
                            },
                            modifier = Modifier.testTag(
                                "${testTagPrefix}_toggle_${option.storageId}"
                            ),
                        ) {
                            Text(if (isAdded) "Remove" else "Add")
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
        Text(
            "The quick picker can hold up to ${GoogleNearbyPlaceTypeCatalog.MAX_PICKER_TYPES} types. " +
                "Typing here does not contact Google.",
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private const val MAX_QUERY_LENGTH = 60
