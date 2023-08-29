package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.favoriteplaces.feature_favorites.presentation.util.PermissionAction



@Composable
fun PermissionUI(
    context: Context,
    permission: String,
    permissionRationale: String,
    scaffoldState: ScaffoldState,
    permissionAction: (PermissionAction) -> Unit
) {
    val permissionGranted =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    if (permissionGranted) {
        Log.d(TAG, "Permission already granted, exiting..")
        permissionAction(PermissionAction.OnPermissionGranted)
        return
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Permission provided by user")
            // Permission Accepted
            permissionAction(PermissionAction.OnPermissionGranted)
        } else {
            Log.d(TAG, "Permission denied by user")
            // Permission Denied
            permissionAction(PermissionAction.OnPermissionDenied)
        }
    }

    val showPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)

    if (showPermissionRationale) {
        Log.d(TAG, "Showing permission rationale for $permission")

        LaunchedEffect(showPermissionRationale) {
            val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                message = permissionRationale,
                actionLabel = "Grant Access",
                duration = SnackbarDuration.Long
            )
            when (snackbarResult) {
                SnackbarResult.Dismissed -> {
                    Log.d(TAG, "User dismissed permission rationale for $permission")
                    // User denied the permission, do nothing
                    permissionAction(PermissionAction.OnPermissionDenied)
                }
                SnackbarResult.ActionPerformed -> {
                    Log.d(TAG, "User granted permission for $permission rationale. Launching permission request..")
                    launcher.launch(permission)
                }
            }
        }
    } else {
        // Request permissions again
        Log.d(TAG, "Requesting permission for $permission again")
        SideEffect {
            launcher.launch(permission)
        }
    }

}
