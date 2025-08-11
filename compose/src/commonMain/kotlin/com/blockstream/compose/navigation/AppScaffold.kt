package com.blockstream.compose.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_home
import blockstream_green.common.generated.resources.id_security
import blockstream_green.common.generated.resources.id_settings
import blockstream_green.common.generated.resources.id_transact
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowsDownUp
import com.adamglin.phosphoricons.regular.Gear
import com.adamglin.phosphoricons.regular.House
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.blockstream.common.models.MainViewModel
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenTopAppBar
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.ui.navigation.LocalNavigator
import com.blockstream.ui.navigation.NavData
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KClass

sealed class TopLevelRoute(val name: StringResource, val icon: ImageVector, val klass: KClass<*>) {
    data object Home : TopLevelRoute(
        name = Res.string.id_home,
        icon = PhosphorIcons.Regular.House,
        klass = NavigateDestinations.WalletOverview::class
    )

    data object Transact :
        TopLevelRoute(
            name = Res.string.id_transact,
            icon = PhosphorIcons.Regular.ArrowsDownUp,
            klass = NavigateDestinations.Transact::class
        )

    data object Security :
        TopLevelRoute(
            name = Res.string.id_security,
            icon = PhosphorIcons.Regular.ShieldCheck,
            klass = NavigateDestinations.Security::class
        )

    data object Settings : TopLevelRoute(
        name = Res.string.id_settings,
        icon = PhosphorIcons.Regular.Gear,
        klass = NavigateDestinations.WalletSettings::class
    )
}

val TopLevelRoutes = listOf(
    TopLevelRoute.Home,
    TopLevelRoute.Transact,
    TopLevelRoute.Security,
    TopLevelRoute.Settings
)

@Composable
fun AppScaffold(
    navData: NavData = NavData(),
    snackbarHostState: SnackbarHostState? = null,
    mainViewModel: MainViewModel,
    navigate: (destination: NavigateDestination) -> Unit = {},
    goBack: () -> Unit = { },
    content: @Composable (PaddingValues) -> Unit
) {
    val navigator = LocalNavigator.current
    val navBackStackEntry by navigator.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentBackStack by navigator.currentBackStack.collectAsStateWithLifecycle()
    var showNavigationBar by mutableStateOf(true)

    navigator.addOnDestinationChangedListener { _, destination, _ ->
        showNavigationBar = destination.let {
            TopLevelRoutes.any { topLevelRoute ->
                it.hasRoute(topLevelRoute.klass)
            }
        }
    }

    val greenWallet by remember {
        derivedStateOf {
            currentBackStack.find {
                it.destination.hasRoute<NavigateDestinations.WalletOverview>()
            }?.toRoute<NavigateDestinations.WalletOverview>()?.greenWallet
        }
    }

    // Clear tre backstack when navigating to a new wallet
    LaunchedEffect(greenWallet) {
        // Be sure there is a backstack
        // Fix: Exception java.lang.IllegalStateException: You must call setGraph() before calling getGraph()
        if (navigator.currentBackStackEntry != null) {
            navigator.clearBackStack<NavigateDestinations.WalletOverview>()
            navigator.clearBackStack<NavigateDestinations.Transact>()
            navigator.clearBackStack<NavigateDestinations.Security>()
            navigator.clearBackStack<NavigateDestinations.WalletSettings>()
        }
    }

    // Handle side effects from MainViewModel like navigating from handled intent
    HandleSideEffect(mainViewModel)

    // val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    // val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollBehavior = null

    Scaffold(
        // modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GreenTopAppBar(
                hasBackStack = currentBackStack.size > 2,
                scrollBehavior = scrollBehavior,
                navData = navData,
                goBack = goBack
            )
        }, snackbarHost = {
            snackbarHostState?.also {
                SnackbarHost(
                    // provide ime padding to show the snackbar above the keyboard
                    modifier = Modifier.imePadding(), hostState = it
                )
            }
        }) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {

            // Content
            content(innerPadding)

            // Bottom NavigationBar
            greenWallet?.also { greenWallet ->
                AnimatedVisibility(
                    visible = showNavigationBar && navData.showBottomNavigation && navData.isVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar {
                        TopLevelRoutes.forEach { topLevelRoute ->
                            NavigationBarItem(
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (topLevelRoute is TopLevelRoute.Security && navData.showBadge) {
                                                Badge()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = topLevelRoute.icon,
                                            contentDescription = topLevelRoute.name.key
                                        )
                                    }
                                },
                                label = {
                                    Text(stringResource(topLevelRoute.name), style = bodyMedium)
                                },
                                selected = currentDestination?.hierarchy?.any {
                                    it.hasRoute(
                                        when (topLevelRoute) {
                                            TopLevelRoute.Home -> NavigateDestinations.WalletOverview::class
                                            TopLevelRoute.Transact -> NavigateDestinations.Transact::class
                                            TopLevelRoute.Security -> NavigateDestinations.Security::class
                                            TopLevelRoute.Settings -> NavigateDestinations.WalletSettings::class
                                        }
                                    )
                                } == true,
                                onClick = {
                                    val destination = when (topLevelRoute) {
                                        TopLevelRoute.Home -> NavigateDestinations.WalletOverview(
                                            greenWallet = greenWallet,
                                            isBottomNav = true
                                        )

                                        TopLevelRoute.Transact -> NavigateDestinations.Transact(
                                            greenWallet = greenWallet
                                        )

                                        TopLevelRoute.Security -> NavigateDestinations.Security(
                                            greenWallet = greenWallet
                                        )

                                        TopLevelRoute.Settings -> NavigateDestinations.WalletSettings(
                                            greenWallet = greenWallet
                                        )
                                    }

                                    navigate(destination)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}