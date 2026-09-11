package com.example.favoriteplaces.feature_favorites.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(
    navController: NavController,
    viewModel: DataBackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::export) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::inspectImport) }

    LaunchedEffect(state.message, state.error) {
        val notice = state.error ?: state.message
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    state.pendingImportCount?.let { count ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Import $count places?") },
            text = {
                Text(
                    "Existing places will not be deleted or overwritten. A backup can fill a missing " +
                        "phone number or Google category; if anything else conflicts, the entire import stops without changes."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data & backup") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Keep a portable copy of every saved place.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Android's file picker can save the backup on this phone or in Google Drive. " +
                    "Backups include notes, ratings, hearts, exact colors, addresses, coordinates, phone numbers, and Google categories."
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { exportLauncher.launch("places-backup.json") },
                    enabled = !state.isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export backup")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                    enabled = !state.isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import backup")
                }
            }
            if (state.isWorking) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Text(
                "Import is additive and transactional: it never clears the database, can fill only missing Google details, and a conflict leaves everything unchanged.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
