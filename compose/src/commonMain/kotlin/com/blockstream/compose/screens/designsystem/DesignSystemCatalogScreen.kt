package com.blockstream.compose.screens.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.utils.SetupScreen

internal class CatalogSection(val title: String, val content: @Composable () -> Unit)

// Each new component adds its own section here.
internal val catalogSections = listOf(
    CatalogSection("Colors") { ColorCatalog() },
    CatalogSection("Typography") { TypographyCatalog() },
)

@Composable
fun DesignSystemCatalogScreen(viewModel: GreenViewModel) {
    var isLight by rememberSaveable { mutableStateOf(false) }

    SetupScreen(viewModel = viewModel, withPadding = false) {
        ProvideAppTheme(isLight = isLight) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.surface.canvas),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Light theme",
                            style = AppTheme.typography.label.lMedium,
                            color = AppTheme.colors.text.primary
                        )
                        Switch(checked = isLight, onCheckedChange = { isLight = it })
                    }
                }

                items(catalogSections, key = { it.title }) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = section.title,
                            style = AppTheme.typography.title.lSemiBold,
                            color = AppTheme.colors.text.primary
                        )
                        section.content()
                    }
                }
            }
        }
    }
}
