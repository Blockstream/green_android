package com.blockstream.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppButton
import com.blockstream.designsystem.components.AppButtonDefaults
import com.blockstream.designsystem.components.AppButtonSize
import com.blockstream.designsystem.components.AppButtonState
import com.blockstream.designsystem.components.AppButtonSurface
import com.blockstream.designsystem.components.AppButtonVariant
import com.blockstream.designsystem.components.AppIcon
import com.blockstream.designsystem.components.HexagonIcon
import com.blockstream.designsystem.components.AppText

@Composable
fun ButtonCatalog() {
    var size by rememberSaveable { mutableStateOf(AppButtonSize.LG) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButtonSize.entries.forEach { entry ->
                AppButton(
                    text = entry.name,
                    size = AppButtonSize.XS,
                    variant = if (entry == size) AppButtonVariant.Secondary else AppButtonVariant.Tertiary,
                    onClick = { size = entry }
                )
            }
        }

        AppButtonVariant.entries.forEach { variant ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogLabel(variant.name)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(text = "Default", variant = variant, size = size, onClick = {})
                    AppButtonSurface(state = AppButtonState.Pressed, style = AppButtonDefaults.style(variant, size)) {
                        AppText("Pressed")
                    }
                    AppButton(text = "Disabled", variant = variant, size = size, enabled = false, onClick = {})
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Icons")
            AppButton(text = "Leading", size = size, leadingIcon = HexagonIcon, onClick = {})
            AppButton(text = "Trailing", size = size, trailingIcon = HexagonIcon, onClick = {})
            AppButton(
                text = "Full width",
                size = size,
                variant = AppButtonVariant.Secondary,
                leadingIcon = HexagonIcon,
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
            AppButton(
                text = "A label long enough to run out of room next to the icon",
                size = size,
                trailingIcon = HexagonIcon,
                onClick = {}
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Content slot, no ripple (uses the pressed colours)")
            AppButton(
                size = size,
                variant = AppButtonVariant.Primary,
                rippleEnabled = false,
                onClick = {}
            ) {
                AppIcon(HexagonIcon, contentDescription = null)
                AppText("Custom content")
            }
        }
    }
}

