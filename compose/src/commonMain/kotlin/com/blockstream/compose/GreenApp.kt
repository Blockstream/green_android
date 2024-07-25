package com.blockstream.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import com.blockstream.common.crypto.NoKeystore
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.compose.components.GreenTopAppBar
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.screens.DrawerScreen
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.LockScreen
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.AppBarState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools


val LocalAppBarState = compositionLocalOf { AppBarState() }
val LocalSnackbar = compositionLocalOf { SnackbarHostState() }
val LocalAppCoroutine = compositionLocalOf { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
val LocalDrawer = compositionLocalOf { DrawerState(DrawerValue.Closed) }
val LocalDialog: ProvidableCompositionLocal<DialogState> =
    staticCompositionLocalOf { error("DialogState not initialized") }
val LocalRootNavigator: ProvidableCompositionLocal<Navigator?> = staticCompositionLocalOf { null }
val LocalActivity: ProvidableCompositionLocal<Any?> =
    staticCompositionLocalOf { error("LocalActivity not initialized") }

@Composable
fun GreenApp(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val appBarState = remember { AppBarState() }
    val dialogState = remember { DialogState() }
    val platformManager = rememberPlatformManager()

    CompositionLocalProvider(
        LocalSnackbar provides snackbarHostState,
        LocalAppBarState provides appBarState,
        LocalDrawer provides drawerState,
        LocalDialog provides dialogState,
        LocalPlatformManager provides platformManager
    ) {

        val lifecycleManager = koinInject<LifecycleManager>()
        val isLocked by lifecycleManager.isLocked.collectAsStateWithLifecycle()

        Box(modifier = modifier) {
            Navigator(screen = HomeScreen, onBackPressed = { _ ->
                !isLocked && appBarState.data.value.isVisible && appBarState.data.value.onBackPressed()
            }) { navigator ->

                CompositionLocalProvider(
                    LocalRootNavigator provides navigator
                ) {

                    BottomSheetNavigatorM3 { _ ->

                        CompositionLocalProvider(
                            LocalNavigator provides navigator
                        ) {

                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                drawerContent = {
                                    ModalDrawerSheet(
                                        drawerContainerColor = MaterialTheme.colorScheme.background,
                                    ) {
                                        val drawerViewModel =
                                            navigator.koinNavigatorScreenModel<DrawerViewModel>()
                                        DrawerScreen(viewModel = drawerViewModel)
                                    }
                                }
                            ) {

                                Scaffold(
                                    snackbarHost = {
                                        SnackbarHost(hostState = snackbarHostState)
                                    },
                                    topBar = {
                                        GreenTopAppBar(
                                            openDrawer = {
                                                scope.launch {
                                                    // Open the drawer with animation
                                                    // and suspend until it is fully
                                                    // opened or animation has been canceled
                                                    drawerState.open()
                                                }
                                            }, showDrawer = {
                                                it is HomeScreen || it is LoginScreen || it is WalletOverviewScreen
                                            }
                                        )
                                    },
                                    content = { innerPadding ->
                                        Box(
                                            modifier = Modifier
                                                .padding(innerPadding),
                                        ) {
                                            FadeSlideTransition(navigator)
                                        }
                                    },
                                )

                                DialogHost(state = dialogState)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isLocked,
                enter = EnterTransition.None,
                exit = fadeOut()
            ) {
                LockScreen {
                    lifecycleManager.unlock()
                }
            }
        }
    }
}

@Composable
public fun FadeSlideTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: ScreenTransitionContent = { it.Content() }
) {
    ScreenTransition(
        navigator = navigator,
        modifier = modifier,
        content = content,
        transition = {
            val (initialOffset, targetOffset) = when (navigator.lastEvent) {
                StackEvent.Pop -> ({ size: Int -> -size }) to ({ size: Int -> size })
                else -> ({ size: Int -> size }) to ({ size: Int -> -size })
            }

            val animationSpec: FiniteAnimationSpec<IntOffset> = spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )

            val animationSpecFade: FiniteAnimationSpec<Float> =
                spring(stiffness = Spring.StiffnessMediumLow)

            if (navigator.lastEvent == StackEvent.Pop) {
                fadeIn(animationSpecFade) togetherWith slideOutHorizontally(
                    animationSpec,
                    targetOffset
                )
            } else {
                slideInHorizontally(
                    animationSpec,
                    initialOffset
                ) togetherWith fadeOut(animationSpec = animationSpecFade)
            }
        }
    )
}

@Composable
fun GreenPreview(content: @Composable () -> Unit) {
    val dialogState = remember { DialogState() }
    val platformManager = rememberPlatformManager()

    // startKoin only once
    KoinPlatformTools.defaultContext().getOrNull() ?: startKoin {
        modules(module {
            single {
                NoKeystore()
            }
        })
    }

    GreenTheme {
        CompositionLocalProvider(
            LocalDialog provides dialogState,
            LocalPlatformManager provides platformManager
        ) {
            BottomSheetNavigatorM3 {
                DialogHost(state = dialogState)
                content()
            }
        }
    }
}
