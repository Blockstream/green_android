package com.blockstream.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.components.AppSegmentedControl

@Composable
fun SegmentedControlCatalog() {
    var two by rememberSaveable { mutableIntStateOf(0) }
    var three by rememberSaveable { mutableIntStateOf(1) }
    var five by rememberSaveable { mutableIntStateOf(2) }
    var hugging by rememberSaveable { mutableIntStateOf(0) }
    var long by rememberSaveable { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Two segments")
            AppSegmentedControl(
                labels = listOf("Mobile", "Hardware"),
                selectedIndex = two,
                onSelect = { two = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Three segments")
            AppSegmentedControl(
                labels = listOf("12", "24", "27"),
                selectedIndex = three,
                onSelect = { three = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Five segments")
            AppSegmentedControl(
                labels = (1..5).map { it.toString() },
                selectedIndex = five,
                onSelect = { five = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Hugs its content")
            Box(modifier = Modifier.fillMaxWidth()) {
                AppSegmentedControl(
                    labels = listOf("Buy", "Sell"),
                    selectedIndex = hugging,
                    onSelect = { hugging = it },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogLabel("Long labels")
            AppSegmentedControl(
                labels = listOf("Hardware wallet", "Mobile wallet", "Watch only"),
                selectedIndex = long,
                onSelect = { long = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
