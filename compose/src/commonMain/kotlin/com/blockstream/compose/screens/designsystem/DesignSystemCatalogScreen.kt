package com.blockstream.compose.screens.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.compose.models.designsystem.DesignSystemCatalogViewModel
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.designsystem.AppTheme
import com.blockstream.designsystem.ProvideAppTheme
import com.blockstream.designsystem.catalog.CatalogEntry
import com.blockstream.designsystem.catalog.CatalogGroup
import com.blockstream.designsystem.catalog.DesignSystemCatalog
import com.blockstream.designsystem.components.AppIcon
import com.blockstream.designsystem.components.AppSurface
import com.blockstream.designsystem.components.AppText

@Composable
fun DesignSystemCatalogScreen(viewModel: DesignSystemCatalogViewModel) {
    val isLight by viewModel.isLight.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, withPadding = false) {
        ProvideAppTheme(isLight = isLight) {
            AppSurface(modifier = Modifier.fillMaxSize(), color = AppTheme.colors.surface.canvas) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogGroup.entries.forEach { group ->
                        val entries = DesignSystemCatalog.entries.filter { it.group == group }
                        if (entries.isEmpty()) return@forEach
                        item(key = "group:${group.name}") {
                            AppText(
                                text = group.title,
                                style = AppTheme.typography.overline.sSemiBold,
                                color = AppTheme.colors.text.subtle,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(entries, key = { "entry:${it.id}" }) { entry ->
                            CatalogRow(entry = entry, onClick = { viewModel.open(entry.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesignSystemCatalogEntryScreen(viewModel: DesignSystemCatalogViewModel) {
    val isLight by viewModel.isLight.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, withPadding = false, withImePadding = true) {
        ProvideAppTheme(isLight = isLight) {
            AppSurface(modifier = Modifier.fillMaxSize(), color = AppTheme.colors.surface.canvas) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val entry = viewModel.entry
                    if (entry != null) entry.content() else AppText("Unknown catalog page: ${viewModel.entryId}")
                }
            }
        }
    }
}

@Composable
private fun CatalogRow(entry: CatalogEntry, onClick: () -> Unit) {
    AppSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = AppTheme.colors.surface.raised
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(text = entry.title, style = AppTheme.typography.label.lMedium, modifier = Modifier.weight(1f))
            AppIcon(PhosphorIcons.Regular.CaretRight, contentDescription = null, tint = AppTheme.colors.icon.subtle)
        }
    }
}
