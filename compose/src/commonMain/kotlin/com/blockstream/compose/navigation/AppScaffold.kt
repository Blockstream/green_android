package com.blockstream.compose.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_home
import blockstream_green.common.generated.resources.id_security
import blockstream_green.common.generated.resources.id_transact
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowsDownUp
import com.adamglin.phosphoricons.regular.House
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.blockstream.common.data.NavData
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.LocalAppInfo
import com.blockstream.compose.components.GreenTopAppBar
import com.blockstream.ui.utils.plus
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
//    data object Settings :
//        TopLevelRoute(name = Res.string.id_settings, icon = PhosphorIcons.Regular.Gear, klass = NavigateDestinations.AppSettings::class)
}

val TopLevelRoutes = listOf(
    TopLevelRoute.Home,
    TopLevelRoute.Transact,
    TopLevelRoute.Security,
//    TopLevelRoute.Settings
)

@Composable
fun AppScaffold(
    navData: NavData = NavData(),
    snackbarHostState: SnackbarHostState? = null,
    showDrawerNavigationIcon: Boolean = false,
    goBack: () -> Unit = { },
    openDrawer: () -> Unit = { },
    content: @Composable (PaddingValues) -> Unit
) {
    val appInfo = LocalAppInfo.current
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

    Scaffold(
        topBar = {
            GreenTopAppBar(
                showDrawerNavigationIcon = showDrawerNavigationIcon,
                navData = navData,
                goBack = goBack,
                openDrawer = openDrawer
            )
        },
        snackbarHost = {
            snackbarHostState?.also {
                SnackbarHost(hostState = it)
            }
        }
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {

            // Content
            content(innerPadding)

            // Bottom NavigationBar
            if (appInfo.enableNewFeatures) {
                greenWallet?.also { greenWallet ->
                    AnimatedVisibility(
                        visible = showNavigationBar,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                    ) {
                        NavigationBar {
                            TopLevelRoutes.forEach { topLevelRoute ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = topLevelRoute.icon,
                                            contentDescription = topLevelRoute.name.key
                                        )
                                    },
                                    label = { Text(stringResource(topLevelRoute.name)) },
                                    selected = currentDestination?.hierarchy?.any {
                                        it.hasRoute(
                                            when (topLevelRoute) {
                                                TopLevelRoute.Home -> NavigateDestinations.WalletOverview::class
                                                TopLevelRoute.Transact -> NavigateDestinations.Transact::class
                                                TopLevelRoute.Security -> NavigateDestinations.Security::class
                                                // TopLevelRoute.Settings -> NavigateDestinations.AppSettings::class
                                            }
                                        )
                                    } == true,
                                    onClick = {
                                        navigator.navigate(
                                            when (topLevelRoute) {
                                                TopLevelRoute.Home -> NavigateDestinations.WalletOverview(
                                                    greenWallet = greenWallet
                                                )

                                                TopLevelRoute.Transact -> NavigateDestinations.Transact(
                                                    greenWallet = greenWallet
                                                )

                                                TopLevelRoute.Security -> NavigateDestinations.Security(
                                                    greenWallet = greenWallet
                                                )
                                                // TopLevelRoute.Settings -> NavigateDestinations.AppSettings
                                            }
                                        ) {
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
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}