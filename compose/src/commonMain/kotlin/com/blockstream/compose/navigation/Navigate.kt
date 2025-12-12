package com.blockstream.compose.navigation

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.toRoute

fun navigate(
    navigator: NavHostController,
    destination: NavigateDestination
) {

    navigator.navigate(destination) {
        // Clear all previous Recovery screens if needed (eg. WalletSettings > RecoveryIntroScreen)
        (when (destination) {
            is NavigateDestinations.RecoveryCheck, is NavigateDestinations.SetPin, is NavigateDestinations.ReviewAddAccount -> false
            is NavigateDestinations.RecoveryPhrase -> true
            else -> null
        })?.also { popUpToRecoveryIntroInclusive ->
            navigator.currentBackStack.value.firstOrNull { entry ->
                entry.destination.hasRoute<NavigateDestinations.RecoveryIntro>()
            }?.toRoute<NavigateDestinations.RecoveryIntro>()?.also { route ->
                popUpTo(route) {
                    inclusive = popUpToRecoveryIntroInclusive
                }
            }
        }

        if (destination is NavigateDestinations.ImportPubKey) {
            navigator.currentBackStack.value.firstOrNull { entry ->
                entry.destination.hasRoute<NavigateDestinations.JadePinUnlock>()
            }?.toRoute<NavigateDestinations.JadePinUnlock>()?.also { route ->
                popUpTo(route) {
                    inclusive = true
                }
            }
        }

        (destination as? NavigateDestinations.Login)?.also { destination ->
            if (destination.isWatchOnlyUpgrade) {
                popUpTo(NavigateDestinations.DeviceScan::class) {
                    inclusive = true
                }
            }

            navigator.currentBackStack.value.firstOrNull { entry ->
                entry.destination.hasRoute<NavigateDestinations.DeviceList>()
            }?.toRoute<NavigateDestinations.DeviceList>()?.also { route ->
                popUpTo(route) {
                    inclusive = false
                }
            }
        }

        if (destination.isBottomNavigation) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navigator.graph.findStartDestination().id) {
                inclusive = false
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        } else {

            if (destination.unique) {
                // Same route
                if (navigator.currentBackStackEntry?.destination?.hasRoute(destination::class) == true) {
                    navigator.currentBackStackEntry?.destination?.id?.also {
                        popUpTo(it) {
                            inclusive = true
                        }
                    }
                }
            }

            if (destination.makeItRoot) {
                popUpTo(navigator.graph.id) {
                    inclusive = false
                }

                navigator.graph.setStartDestination(destination)
            }
        }
    }
}