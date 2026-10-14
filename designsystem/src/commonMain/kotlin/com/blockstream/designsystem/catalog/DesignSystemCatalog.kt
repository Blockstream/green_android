package com.blockstream.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

enum class CatalogGroup(val title: String) {
    Foundation("Foundation"),
    Components("Components")
}

@Immutable
class CatalogEntry(
    val id: String,
    val title: String,
    val group: CatalogGroup,
    val content: @Composable () -> Unit
)

object DesignSystemCatalog {
    val entries: List<CatalogEntry> = listOf(
        CatalogEntry(id = "colors", title = "Colors", group = CatalogGroup.Foundation) { ColorCatalog() },
        CatalogEntry(id = "typography", title = "Typography", group = CatalogGroup.Foundation) { TypographyCatalog() },
        CatalogEntry(id = "button", title = "Button", group = CatalogGroup.Components) { ButtonCatalog() },
        CatalogEntry(id = "textfield", title = "Text field", group = CatalogGroup.Components) { TextFieldCatalog() },
        CatalogEntry(id = "switch", title = "Switch", group = CatalogGroup.Components) { SwitchCatalog() },
        CatalogEntry(id = "checkbox", title = "Checkbox", group = CatalogGroup.Components) { CheckboxCatalog() },
        CatalogEntry(id = "radio", title = "Radio", group = CatalogGroup.Components) { RadioCatalog() }
    )

    fun entry(id: String): CatalogEntry? = entries.firstOrNull { it.id == id }
}
