package com.blockstream.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal actual fun AppBottomSheetWindow(
    onDismissRequest: () -> Unit,
    properties: AppBottomSheetProperties,
    contentColor: Color,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = properties.shouldDismissOnBackPress,
            dismissOnClickOutside = properties.shouldDismissOnClickOutside,
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            useSoftwareKeyboardInset = true,
            scrimColor = Color.Transparent
        ),
        content = content
    )
}
