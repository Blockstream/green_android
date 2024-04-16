package com.blockstream.compose.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.md_theme_surfaceCircle

@Composable
fun Modifier.noRippleToggleable(
    value: Boolean,
    enabled: Boolean = true,
    onValueChange: (Boolean) -> Unit
): Modifier {
    return this then toggleable(
        value = value,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onValueChange = onValueChange
    )
}

@Composable
inline fun Modifier.noRippleClickable(
    crossinline onClick: () -> Unit
): Modifier {
    return this then clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        onClick()
    }
}

@Composable
inline fun Modifier.roundBackground(
    horizontal: Dp = 8.dp,
    vertical: Dp = 4.dp,
    size : Dp = 16.dp,
    color: Color = md_theme_surfaceCircle
): Modifier {
    return this then background(
        color = color,
        shape = RoundedCornerShape(size)
    ).padding(horizontal = horizontal, vertical = vertical)
}