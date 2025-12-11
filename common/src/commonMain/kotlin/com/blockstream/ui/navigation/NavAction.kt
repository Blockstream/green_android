package com.blockstream.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class NavAction(
    val title: String? = null,
    val titleRes: StringResource? = null,
    val icon: DrawableResource? = null,
    val imageVector: ImageVector? = null,
    val isMenuEntry: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit = { }
)
