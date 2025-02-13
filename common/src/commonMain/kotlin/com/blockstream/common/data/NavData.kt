package com.blockstream.common.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import nl.jacobras.composeactionmenu.RegularActionItem
import org.jetbrains.compose.resources.DrawableResource

data class NavData(
    val title: String? = null,
    val subtitle: String? = null,

    val isVisible: Boolean = true,
    val backHandlerEnabled: Boolean = false,

    val showNavigationIcon: Boolean = true,
    val actions: List<NavAction> = listOf(),
    val actionsMenu: List<RegularActionItem> = emptyList(),
)

data class NavAction(
    val title: String,
    val icon: DrawableResource? = null,
    val isMenuEntry: Boolean = false,
    val onClick: () -> Unit = { }
)

@Stable
class NavDataState(navData: NavData = NavData()) {
    var data = mutableStateOf(navData)
        private set

    fun update(data: NavData) {
        this.data.value = data
    }
}

val LocalNavData = compositionLocalOf { NavDataState() }