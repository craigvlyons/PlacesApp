package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.favoriteplaces.feature_favorites.presentation.util.PermissionAction



@Composable
fun PermissionUI(
    context: Context,
    permissions: List<String>,
    permissionRationale: String,
    snackbarHostState: SnackbarHostState,
    permissionAction: (PermissionAction) -> Unit
) {
    require(permissions.isNotEmpty()) { "At least one permission is required." }
    val permissionGranted = permissions.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    if (permissionGranted) {
        LaunchedEffect(permissions) {
            permissionAction(PermissionAction.OnPermissionGranted)
        }
        return
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        if (grantResults.values.any { it }) {
            permissionAction(PermissionAction.OnPermissionGranted)
        } else {
            permissionAction(PermissionAction.OnPermissionDenied)
        }
    }

    val activity = context.findActivity()
    val showPermissionRationale = activity?.let { owner ->
        permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(owner, permission)
        }
    } == true

    if (showPermissionRationale) {
        LaunchedEffect(showPermissionRationale) {
            val snackbarResult = snackbarHostState.showSnackbar(
                message = permissionRationale,
                actionLabel = "Grant Access",
                duration = SnackbarDuration.Long
            )
            when (snackbarResult) {
                SnackbarResult.Dismissed -> {
                    permissionAction(PermissionAction.OnPermissionDenied)
                }
                SnackbarResult.ActionPerformed -> {
                    launcher.launch(permissions.toTypedArray())
                }
            }
        }
    } else {
        LaunchedEffect(permissions) {
            launcher.launch(permissions.toTypedArray())
        }
    }

}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
