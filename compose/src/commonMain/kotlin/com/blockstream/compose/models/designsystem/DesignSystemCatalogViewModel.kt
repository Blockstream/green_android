package com.blockstream.compose.models.designsystem

import com.blockstream.compose.utils.toStringHolder
import com.blockstream.compose.events.Event
import androidx.lifecycle.viewModelScope
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.designsystem.catalog.DesignSystemCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DesignSystemCatalogViewModel(val entryId: String? = null, initialIsLight: Boolean = false) : GreenViewModel() {

    val entry = entryId?.let { DesignSystemCatalog.entry(it) }

    val isLight = MutableStateFlow(initialIsLight)

    init {
        isLight.onEach { light ->
            _navData.value = NavData(
                title = (entry?.title ?: "Design system").toStringHolder(),
                actions = listOf(
                    NavAction.ThemeToggle(isLight = light, testTag = "btn_catalog_theme", event = { LocalEvents.ToggleTheme })
                )
            )
        }.launchIn(viewModelScope)

        bootstrap()
    }

    class LocalEvents {
        object ToggleTheme : Event
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ToggleTheme) {
            isLight.value = !isLight.value
        }
    }

    fun open(entryId: String) {
        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.DesignSystemCatalogEntry(entryId, isLight = isLight.value)))
    }
}
