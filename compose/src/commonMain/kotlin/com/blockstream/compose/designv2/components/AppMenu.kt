package com.blockstream.compose.designv2.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.blockstream.designsystem.AppRadius
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppIcon
import com.blockstream.designsystem.components.AppText

@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.lg),
        containerColor = AppTheme.colors.surface.overlay,
        content = content
    )
}

@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        text = {
            AppText(
                text = text,
                style = AppTheme.typography.label.mMedium,
                color = if (enabled) AppTheme.colors.text.primary else AppTheme.colors.text.disabled
            )
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = icon?.let {
            {
                AppIcon(
                    it,
                    contentDescription = null,
                    tint = if (enabled) AppTheme.colors.icon.base else AppTheme.colors.icon.disabled
                )
            }
        }
    )
}
