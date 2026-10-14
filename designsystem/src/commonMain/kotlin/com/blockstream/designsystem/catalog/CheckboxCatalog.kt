package com.blockstream.designsystem.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.components.AppCheckbox
import com.blockstream.designsystem.components.AppCheckboxVariant
import com.blockstream.designsystem.components.AppSurface
import com.blockstream.designsystem.components.AppText

@Composable
fun CheckboxCatalog() {
    var first by rememberSaveable { mutableStateOf(false) }
    var second by rememberSaveable { mutableStateOf(true) }
    var third by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(false) }
    var coin by rememberSaveable { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("States")
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                AppCheckbox(checked = first, onCheckedChange = { first = it })
                AppCheckbox(checked = second, onCheckedChange = { second = it })
                AppCheckbox(checked = false, onCheckedChange = {}, enabled = false)
                AppCheckbox(checked = true, onCheckedChange = {}, enabled = false)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("List variant, grey until checked")
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                AppCheckbox(checked = third, onCheckedChange = { third = it }, variant = AppCheckboxVariant.List)
                AppCheckbox(checked = true, onCheckedChange = {}, variant = AppCheckboxVariant.List)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("With a label, the whole row toggles")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = terms, onValueChange = { terms = it }, role = Role.Checkbox)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppCheckbox(checked = terms, onCheckedChange = null)
                AppText("I have read and agree to the Terms of Service", style = AppTheme.typography.label.m)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("In a card")
            AppSurface(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AppTheme.colors.border.subtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .toggleable(value = coin, onValueChange = { coin = it }, role = Role.Checkbox)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AppText("0.00120000 BTC", style = AppTheme.typography.label.lMedium)
                        AppText("bc1qxy2k...fqz9", style = AppTheme.typography.code.s, color = AppTheme.colors.text.subtle)
                    }
                    AppCheckbox(checked = coin, onCheckedChange = null, variant = AppCheckboxVariant.List)
                }
            }
        }
    }
}

