package com.blockstream.designsystem.catalog

import androidx.compose.runtime.Composable
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppText

@Composable
internal fun CatalogLabel(text: String) {
    AppText(text = text, style = AppTheme.typography.label.mMedium, color = AppTheme.colors.text.subtle)
}
