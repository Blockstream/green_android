package com.blockstream.compose.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class IconContainer {
    NONE, OUTLINED
}

sealed interface NavAction {
    data class Text(
        val title: String? = null,
        val titleRes: StringResource? = null,
        val icon: DrawableResource? = null,
        val imageVector: ImageVector? = null,
        val iconTint: Color? = null,
        val iconSize: Dp = 18.dp,
        val testTag: String? = null,
        val enabled: Boolean = true,
        val onClick: () -> Unit = { }
    ) : NavAction

    data class Icon(
        val title: String? = null,
        val titleRes: StringResource? = null,
        val icon: DrawableResource? = null,
        val imageVector: ImageVector? = null,
        val iconTint: Color? = null,
        val iconSize: Dp = 24.dp,
        val container: IconContainer = IconContainer.NONE,
        val testTag: String? = null,
        val enabled: Boolean = true,
        val onClick: () -> Unit = { }
    ) : NavAction

    data class Menu(
        val title: String? = null,
        val titleRes: StringResource? = null,
        val icon: DrawableResource? = null,
        val imageVector: ImageVector? = null,
        val testTag: String? = null,
        val enabled: Boolean = true,
        val onClick: () -> Unit = { }
    ) : NavAction

    // TODO: an Avatar action is coming for the profile button
}
