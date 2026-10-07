package com.blockstream.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.AppTypography
import com.blockstream.designsystem.components.AppText

private const val SAMPLE = "The quick brown fox jumps over the lazy dog"

@Composable
fun TypographyCatalog() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppTheme.typography.catalogGroups().forEach { (group, styles) ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppText(text = group, style = AppTheme.typography.title.sSemiBold, color = AppTheme.colors.text.primary)
                styles.forEach { (name, style) -> TypographySample(name = name, style = style, uppercase = group == "Overline") }
            }
        }
    }
}

@Composable
private fun TypographySample(name: String, style: TextStyle, uppercase: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppText(
            text = "$name, ${style.fontSize.value.toInt()}/${style.lineHeight.value.toInt()}",
            style = AppTheme.typography.code.s,
            color = AppTheme.colors.text.subtle
        )
        AppText(
            text = if (uppercase) SAMPLE.uppercase() else SAMPLE,
            style = style,
            color = AppTheme.colors.text.primary
        )
    }
}

private fun AppTypography.catalogGroups(): List<Pair<String, List<Pair<String, TextStyle>>>> = listOf(
    "Body" to listOf(
        "body.s" to body.s,
        "body.m" to body.m,
        "body.l" to body.l
    ),
    "Label" to listOf(
        "label.xs" to label.xs,
        "label.sm" to label.sm,
        "label.smSemiBold" to label.smSemiBold,
        "label.m" to label.m,
        "label.mMedium" to label.mMedium,
        "label.mSemiBold" to label.mSemiBold,
        "label.l" to label.l,
        "label.lMedium" to label.lMedium,
        "label.lSemiBold" to label.lSemiBold
    ),
    "Title" to listOf(
        "title.xsBold" to title.xsBold,
        "title.sBold" to title.sBold,
        "title.sSemiBold" to title.sSemiBold,
        "title.m" to title.m,
        "title.mSemiBold" to title.mSemiBold,
        "title.lMedium" to title.lMedium,
        "title.lSemiBold" to title.lSemiBold,
        "title.lBold" to title.lBold,
        "title.xl" to title.xl,
        "title.xlMedium" to title.xlMedium,
        "title.xlSemiBold" to title.xlSemiBold,
        "title.xxlBold" to title.xxlBold
    ),
    "Code" to listOf(
        "code.s" to code.s,
        "code.m" to code.m
    ),
    "Overline" to listOf(
        "overline.xs" to overline.xs,
        "overline.s" to overline.s,
        "overline.sSemiBold" to overline.sSemiBold
    ),
    "Display" to listOf(
        "display.sMedium" to display.sMedium,
        "display.mMedium" to display.mMedium,
        "display.mSemiBold" to display.mSemiBold,
        "display.lSemiBold" to display.lSemiBold,
        "display.xlMedium" to display.xlMedium
    )
)
