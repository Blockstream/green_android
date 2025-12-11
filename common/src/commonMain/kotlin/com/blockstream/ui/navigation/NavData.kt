package com.blockstream.ui.navigation

import org.jetbrains.compose.resources.StringResource

data class NavData(
    val title: String? = null,
    val titleRes: StringResource? = null,

    val subtitle: String? = null,

    val walletName: String? = null,

    val isVisible: Boolean = true,
    val backHandlerEnabled: Boolean = false,
    val onBackClicked: (() -> Unit)? = null,

    val showBadge: Boolean = false,
    val showBottomNavigation: Boolean = false,

    val showNavigationIcon: Boolean = true,
    val actions: List<NavAction> = listOf(),
    // val actionsMenu: List<RegularActionItem> = emptyList(),
)