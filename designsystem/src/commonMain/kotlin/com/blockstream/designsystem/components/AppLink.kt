package com.blockstream.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Light
import com.adamglin.phosphoricons.light.ArrowSquareOut
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class AppLinkSize {
    M,
    L
}

private const val PressedContentAlpha = 0.72f
private const val HoveredContentAlpha = 0.84f

@Composable
fun AppLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: AppLinkSize = AppLinkSize.L,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
) {
    val defaultInteractionSource = remember { MutableInteractionSource() }
    val resolvedInteractionSource = interactionSource ?: defaultInteractionSource
    val isPressed by resolvedInteractionSource.collectIsPressedAsState()
    val isHovered by resolvedInteractionSource.collectIsHoveredAsState()
    val targetAlpha = when {
        isPressed -> PressedContentAlpha
        isHovered -> HoveredContentAlpha
        else -> 1f
    }
    val contentAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        label = "AppLinkContentAlpha"
    )
    val color = if (enabled) AppTheme.colors.text.brand else AppTheme.colors.text.disabled
    val style = when (size) {
        AppLinkSize.M -> AppTheme.typography.label.mMedium
        AppLinkSize.L -> AppTheme.typography.label.lMedium
    }

    Row(
        modifier = modifier
            .alpha(contentAlpha)
            .clickable(
                interactionSource = resolvedInteractionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(
            text = text,
            style = style,
            color = color,
            textDecoration = TextDecoration.Underline
        )
        trailingIcon?.let {
            AppIcon(it, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

@Preview
@Composable
private fun AppLinkPreview() {
    ProvideAppTheme(isLight = false) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLink(text = "Learn more", onClick = {}, trailingIcon = PhosphorIcons.Light.ArrowSquareOut)
                AppLink(
                    text = "Learn more",
                    onClick = {},
                    size = AppLinkSize.M,
                    trailingIcon = PhosphorIcons.Light.ArrowSquareOut
                )
                AppLink(text = "Learn more", onClick = {}, enabled = false)
            }
        }
    }
}
