package com.blockstream.designsystem.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppIcon
import com.blockstream.designsystem.components.AppSurface
import com.blockstream.designsystem.components.AppSwitch
import com.blockstream.designsystem.components.AppText
import com.blockstream.designsystem.components.HexagonIcon

@Composable
fun SwitchCatalog() {
    var first by rememberSaveable { mutableStateOf(true) }
    var second by rememberSaveable { mutableStateOf(false) }
    var experimental by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("States")
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                AppSwitch(checked = first, onCheckedChange = { first = it })
                AppSwitch(checked = second, onCheckedChange = { second = it })
                AppSwitch(checked = true, onCheckedChange = {}, enabled = false)
                AppSwitch(checked = false, onCheckedChange = {}, enabled = false)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("In a settings row")
            SettingsRow(
                title = "Enable Experimental Features",
                subtitle = "Experimental features might change, break, or be discontinued at any time, so you agree to use them at your own risk.",
                checked = experimental,
                onCheckedChange = { experimental = it }
            )
            SettingsRow(
                title = "Email",
                subtitle = "ja***@gm**",
                checked = email,
                onCheckedChange = { email = it },
                icon = { AppIcon(HexagonIcon, contentDescription = null, modifier = Modifier.size(24.dp), tint = AppTheme.colors.icon.subtle) }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)? = null
) {
    AppSurface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AppTheme.colors.border.subtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AppText(title, style = AppTheme.typography.label.lMedium)
                AppText(subtitle, style = AppTheme.typography.label.m, color = AppTheme.colors.text.subtle)
            }
            AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

