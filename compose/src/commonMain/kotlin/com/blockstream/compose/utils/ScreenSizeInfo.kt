package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/** Getting screen size info for UI-related calculations */
data class ScreenSizeInfo(val heightPx: Int, val widthPx: Int, val heightDp: Dp, val widthDp: Dp)

@Composable
expect fun getScreenSizeInfo(): ScreenSizeInfo