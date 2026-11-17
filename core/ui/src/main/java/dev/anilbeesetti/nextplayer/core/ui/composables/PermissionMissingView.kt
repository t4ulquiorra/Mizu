package dev.anilbeesetti.nextplayer.core.ui.composables

import androidx.compose.runtime.Composable

@Composable
fun PermissionMissingView(
    isGranted: Boolean,
    showRationale: Boolean,
    permission: String,
    launchPermissionRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isGranted) {
        content()
    } else {
        WelcomePermissionView(
            onPermissionGranted = launchPermissionRequest,
        )
    }
}
