package com.blockstream.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppColors
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppText

@Composable
fun ColorCatalog() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppTheme.colors.catalogGroups().forEach { (group, colors) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(text = group, style = AppTheme.typography.title.sSemiBold, color = AppTheme.colors.text.primary)
                colors.forEach { (name, color) -> ColorSwatch(name = name, color = color) }
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color) {
    val shape = RoundedCornerShape(8.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, shape)
                .border(1.dp, AppTheme.colors.border.subtle, shape)
        )
        Column {
            AppText(text = name, style = AppTheme.typography.label.mMedium, color = AppTheme.colors.text.primary)
            AppText(text = color.hex(), style = AppTheme.typography.code.s, color = AppTheme.colors.text.subtle)
        }
    }
}

private fun Color.hex(): String {
    val rgb = "#" + toArgb().toUInt().toString(16).uppercase().padStart(8, '0').substring(2)
    return if (alpha == 1f) rgb else "$rgb, alpha ${(alpha * 100).toInt()}%"
}

private fun AppColors.catalogGroups(): List<Pair<String, List<Pair<String, Color>>>> = listOf(
    "Text" to listOf(
        "text.brand" to text.brand,
        "text.brandHover" to text.brandHover,
        "text.disabled" to text.disabled,
        "text.inverse" to text.inverse,
        "text.primary" to text.primary,
        "text.subtle" to text.subtle
    ),
    "Surface" to listOf(
        "surface.base" to surface.base,
        "surface.canvas" to surface.canvas,
        "surface.overlay" to surface.overlay,
        "surface.raised" to surface.raised,
        "surface.scrim" to surface.scrim,
        "surface.sunken" to surface.sunken,
        "surface.segmented.selected" to surface.segmented.selected
    ),
    "Border" to listOf(
        "border.bold" to border.bold,
        "border.brand" to border.brand,
        "border.inverse" to border.inverse,
        "border.subtle" to border.subtle
    ),
    "Icon" to listOf(
        "icon.base" to icon.base,
        "icon.brand" to icon.brand,
        "icon.disabled" to icon.disabled,
        "icon.inverse" to icon.inverse,
        "icon.subtle" to icon.subtle
    ),
    "Action" to listOf(
        "action.disabled" to action.disabled,
        "action.onPrimary" to action.onPrimary,
        "action.onPrimaryInverted" to action.onPrimaryInverted,
        "action.pressed" to action.pressed,
        "action.primary" to action.primary,
        "action.primaryInverted" to action.primaryInverted,
        "action.primaryInvertedDisabled" to action.primaryInvertedDisabled,
        "action.primaryInvertedPressed" to action.primaryInvertedPressed
    ),
    "Feedback" to listOf(
        "feedback.info.accent" to feedback.info.accent,
        "feedback.info.fill" to feedback.info.fill,
        "feedback.info.onFill" to feedback.info.onFill,
        "feedback.info.stroke" to feedback.info.stroke,
        "feedback.negative.accent" to feedback.negative.accent,
        "feedback.negative.fill" to feedback.negative.fill,
        "feedback.negative.onFill" to feedback.negative.onFill,
        "feedback.negative.stroke" to feedback.negative.stroke,
        "feedback.positive.accent" to feedback.positive.accent,
        "feedback.positive.fill" to feedback.positive.fill,
        "feedback.positive.onFill" to feedback.positive.onFill,
        "feedback.positive.stroke" to feedback.positive.stroke,
        "feedback.warning.accent" to feedback.warning.accent,
        "feedback.warning.fill" to feedback.warning.fill,
        "feedback.warning.onFill" to feedback.warning.onFill,
        "feedback.warning.stroke" to feedback.warning.stroke
    ),
    "Overlay" to listOf(
        "overlay.scrim" to overlay.scrim
    )
)
