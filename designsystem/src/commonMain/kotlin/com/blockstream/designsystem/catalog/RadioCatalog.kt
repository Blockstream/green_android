package com.blockstream.designsystem.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.blockstream.designsystem.components.AppRadio
import com.blockstream.designsystem.components.AppSurface
import com.blockstream.designsystem.components.AppText
import com.blockstream.designsystem.foundation.appRipple

private val Periods = listOf("6 months", "12 months", "15 months")
private val Currencies = listOf("Bitcoin (BTC)", "Satoshis (sats)", "US dollar (USD)")

@Composable
fun RadioCatalog() {
    var state by rememberSaveable { mutableStateOf(0) }
    var period by rememberSaveable { mutableStateOf(0) }
    var currency by rememberSaveable { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("States")
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppRadio(selected = state == 0, onClick = { state = 0 })
                AppRadio(selected = state == 1, onClick = { state = 1 })
                AppRadio(selected = false, onClick = {}, enabled = false)
                AppRadio(selected = true, onClick = {}, enabled = false)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Settings cards, the whole card selects")
            Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Periods.forEachIndexed { index, label ->
                    AppSurface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.border.subtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .selectable(
                                    selected = period == index,
                                    interactionSource = null,
                                    indication = appRipple(),
                                    role = Role.RadioButton,
                                    onClick = { period = index }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppRadio(selected = period == index, onClick = null)
                            AppText(label, style = AppTheme.typography.label.lMedium)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Dialog list")
            Column(modifier = Modifier.selectableGroup()) {
                Currencies.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currency == index,
                                interactionSource = null,
                                indication = appRipple(),
                                role = Role.RadioButton,
                                onClick = { currency = index }
                            )
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppRadio(selected = currency == index, onClick = null)
                        AppText(label, style = AppTheme.typography.label.m)
                    }
                }
            }
        }
    }
}
