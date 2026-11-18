package com.blockstream.compose.navigation

import com.blockstream.compose.events.Event
import com.blockstream.compose.utils.StringHolder

sealed interface NavAction {
    val event: () -> Event

    data class Help(
        override val event: () -> Event
    ) : NavAction

    data class Note(
        val hasNote: Boolean,
        override val event: () -> Event
    ) : NavAction

    data class InvoiceDescription(
        val hasDescription: Boolean,
        override val event: () -> Event
    ) : NavAction

    data class About(
        val testTag: String? = null,
        override val event: () -> Event
    ) : NavAction

    data class AppSettings(
        val testTag: String? = null,
        override val event: () -> Event
    ) : NavAction

    data class Filter(
        val isActive: Boolean,
        val testTag: String? = null,
        override val event: () -> Event
    ) : NavAction

    data class ThemeToggle(
        val isLight: Boolean,
        val testTag: String? = null,
        override val event: () -> Event
    ) : NavAction

    data class WatchOnly(
        override val event: () -> Event
    ) : NavAction

    data class Troubleshoot(
        override val event: () -> Event
    ) : NavAction

    data class SetupGuide(
        override val event: () -> Event
    ) : NavAction

    data class Region(
        val name: String,
        override val event: () -> Event
    ) : NavAction

    data class Menu(
        val title: StringHolder,
        val icon: MenuIcon? = null,
        val testTag: String? = null,
        val enabled: Boolean = true,
        override val event: () -> Event
    ) : NavAction

    // TODO: an Avatar action is coming for the profile button
}

enum class MenuIcon {
    Info,
    Rename,
    WatchOnly,
    Archive,
    Help,
    Passphrase,
    RecoveryPhrase,
    Delete,
    RequestAmount,
    Addresses,
    Sweep,
    Sign
}
