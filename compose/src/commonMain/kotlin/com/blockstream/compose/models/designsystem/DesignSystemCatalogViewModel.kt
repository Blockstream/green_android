package com.blockstream.compose.models.designsystem

import androidx.lifecycle.viewModelScope
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Sun
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
                title = entry?.title ?: "Design system",
                actions = listOf(
                    NavAction.Icon(
                        title = if (light) "Switch to dark" else "Switch to light",
                        imageVector = if (light) PhosphorIcons.Regular.Moon else PhosphorIcons.Regular.Sun,
                        testTag = "btn_catalog_theme"
                    ) {
                        isLight.value = !light
                    }
                )
            )
        }.launchIn(viewModelScope)

        bootstrap()
    }

    fun open(entryId: String) {
        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.DesignSystemCatalogEntry(entryId, isLight = isLight.value)))
    }
}
