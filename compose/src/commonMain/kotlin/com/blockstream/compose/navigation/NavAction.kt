package com.blockstream.compose.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class NavAction(
    val title: String? = null,
    val titleRes: StringResource? = null,
    val icon: DrawableResource? = null,
    val iconTint: Color? = null,
    val iconSize: Dp = 18.dp,
    val imageVector: ImageVector? = null,
    val isMenuEntry: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit = { }
)
