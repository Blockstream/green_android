package com.blockstream.compose.navigation

import com.blockstream.compose.events.Event
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.gdk.data.Network

data class NavData(
    val title: StringHolder? = null,
    val titleNetwork: Network? = null,
    val titleIconTestTag: String? = null,
    val isCentered: Boolean = false,

    val showBrandLogo: Boolean = false,

    val subtitle: StringHolder? = null,

    val walletName: String? = null,

    val isVisible: Boolean = true,
    val backHandlerEnabled: Boolean = false,
    val backEvent: Event? = null,

    val showBadge: Boolean = false,
    val showBottomNavigation: Boolean = false,

    val showNavigationIcon: Boolean = true,
    val actions: List<NavAction> = listOf(),
)
