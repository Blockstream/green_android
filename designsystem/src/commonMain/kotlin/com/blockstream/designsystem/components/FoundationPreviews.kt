package com.blockstream.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun AppSurfacePreviewDark() = AppSurfacePreview(isLight = false)

@Preview
@Composable
private fun AppSurfacePreviewLight() = AppSurfacePreview(isLight = true)

@Composable
private fun AppSurfacePreview(isLight: Boolean) {
    ProvideAppTheme(isLight = isLight) {
        AppSurface(color = AppTheme.colors.surface.canvas) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppText("Text on the canvas picks up text.primary", style = AppTheme.typography.body.m)

                AppSurface(color = AppTheme.colors.action.primary, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(HexagonIcon, contentDescription = null)
                        AppText("Content on action.primary gets action.onPrimary", style = AppTheme.typography.label.mMedium)
                    }
                }

                AppSurface(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.colors.surface.raised,
                    border = BorderStroke(1.dp, AppTheme.colors.border.subtle)
                ) {
                    AppText("Clickable surface with ripple", modifier = Modifier.padding(12.dp))
                }

                AppSurface(color = AppTheme.colors.feedback.negative.fill, shape = RoundedCornerShape(8.dp)) {
                    AppText("Negative fill gets its onFill colour", modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

internal val HexagonIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Hexagon", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black)) {
            moveTo(12f, 2f)
            lineTo(21f, 7f)
            lineTo(21f, 17f)
            lineTo(12f, 22f)
            lineTo(3f, 17f)
            lineTo(3f, 7f)
            close()
        }
        .build()
}
