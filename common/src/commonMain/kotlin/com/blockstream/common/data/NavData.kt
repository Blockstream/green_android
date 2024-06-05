package com.blockstream.common.data

import org.jetbrains.compose.resources.DrawableResource

data class NavData(
    val title: String? = null,
    val subtitle: String? = null,
    val isVisible: Boolean = true,
    val onBackPressed: () -> Boolean = { true },
    val actions: List<NavAction> = listOf()
)


data class NavAction(
    val title: String,
    val icon: DrawableResource? = null,
    val isMenuEntry: Boolean = false,
    val onClick: () -> Unit = { }
)