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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_do_you_want_to_enable_tor
import blockstream_green.common.generated.resources.id_enable
import blockstream_green.common.generated.resources.id_warning
import blockstream_green.common.generated.resources.id_you_have_clicked_a_payment_uri
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.test.FakeImage
import com.blockstream.common.crypto.NoKeystore
import com.blockstream.common.data.AppInfo
import com.blockstream.common.managers.BluetoothManager
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.models.MainViewModel
import com.blockstream.common.models.drawer.DrawerViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.components.GreenTopAppBar
import com.blockstream.compose.dialogs.UrlWarningDialog
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.screens.DrawerScreen
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.LockScreen
import com.blockstream.compose.screens.devices.DeviceListScreen
import com.blockstream.compose.screens.devices.DeviceScanScreen
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.compose.screens.onboarding.phone.PinScreen
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.compose.screens.promo.PromoScreen
import com.blockstream.compose.screens.recovery.RecoveryCheckScreen
import com.blockstream.compose.screens.recovery.RecoveryIntroScreen
import com.blockstream.compose.screens.recovery.RecoveryPhraseScreen
import com.blockstream.compose.screens.recovery.RecoveryWordsScreen
import com.blockstream.compose.screens.settings.ChangePinScreen
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.sideeffects.BiometricsState
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.sideeffects.rememberBiometricsState
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.AppBarState
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.ifTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools


val LocalAppInfo: ProvidableCompositionLocal<AppInfo> = staticCompositionLocalOf { error("LocalAppInfo not initialized") }
val LocalAppBarState = compositionLocalOf { AppBarState() }
val LocalSnackbar = compositionLocalOf { SnackbarHostState() }
val LocalAppCoroutine = compositionLocalOf { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
val LocalDrawer = compositionLocalOf { DrawerState(DrawerValue.Closed) }
val LocalDialog: ProvidableCompositionLocal<DialogState> =
    staticCompositionLocalOf { error("DialogState not initialized") }
val LocalRootNavigator: ProvidableCompositionLocal<Navigator?> = staticCompositionLocalOf { null }
val LocalActivity: ProvidableCompositionLocal<Any?> = compositionLocalOf { null }
val LocalPreview = compositionLocalOf { false }
val LocalBiometricState: ProvidableCompositionLocal<BiometricsState?> = compositionLocalOf { null }

private val SecureScreens = listOf(
    RecoveryIntroScreen::class,
    RecoveryCheckScreen::class,
    RecoveryWordsScreen::class,
    RecoveryPhraseScreen::class,
    LoginScreen::class,
    PinScreen::class,
    ChangePinScreen::class
)

@Composable
fun GreenApp(mainViewModel: MainViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val appBarState = remember { AppBarState() }
    val dialogState = remember { DialogState() }
    val platformManager = rememberPlatformManager()
    val appInfo = koinInject<AppInfo>()

    CompositionLocalProvider(
        LocalSnackbar provides snackbarHostState,
        LocalAppBarState provides appBarState,
        LocalDrawer provides drawerState,
        LocalDialog provides dialogState,
        LocalPlatformManager provides platformManager,
        LocalAppInfo provides appInfo
    ) {

        val biometricsState = rememberBiometricsState()
        val deviceManager = koinInject<DeviceManager>()
        val isLocked by mainViewModel.lockScreen.collectAsStateWithLifecycle()

        Box {

            Navigator(screen = HomeScreen, onBackPressed = { _ ->
                !isLocked && appBarState.data.value.onBackPressed()
            }) { navigator ->

                val settings by mainViewModel.settingsManager.appSettingsStateFlow.collectAsStateWithLifecycle()

                LaunchedEffect(navigator.lastItemOrNull) {
                    val currentScreen = navigator.lastItemOrNull
                    if (currentScreen?.let { it is DeviceListScreen || it is DeviceScanScreen } == true) {
                        deviceManager.startDeviceDiscovery()
                    } else {
                        deviceManager.stopDeviceDiscovery()
                    }

                    // If enhancedPrivacy is turned off, secure only specific screens
                    if(!settings.enhancedPrivacy) {
                        // Set secure screen based on current screen
                        platformManager.setSecureScreen(SecureScreens.any { it.isInstance(currentScreen) })
                    }
                }

                // Set secure screen based on enhanced privacy setting
                LaunchedEffect(settings.enhancedPrivacy) {
                    // Skip changing secure screen if we are on a secure fragment
                    if(settings.enhancedPrivacy || !SecureScreens.any { it.isInstance(navigator.lastItemOrNull) }){
                        platformManager.setSecureScreen(settings.enhancedPrivacy)
                    }
                }

                val pendingUri by mainViewModel.sessionManager.pendingUri.collectAsStateWithLifecycle()

                LaunchedEffect(pendingUri) {
                    pendingUri?.also {
                        if (navigator.lastItemOrNull is HomeScreen) {
                            val result = snackbarHostState.showSnackbar(
                                message = getString(Res.string.id_you_have_clicked_a_payment_uri),
                                actionLabel = getString(Res.string.id_cancel),
                                duration = SnackbarDuration.Short
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                mainViewModel.sessionManager.pendingUri.value = null
                            }
                        }
                    }
                }

                CompositionLocalProvider(
                    LocalRootNavigator provides navigator,
                    LocalBiometricState provides biometricsState
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

                                        var showUrlWarning by remember {
                                            mutableStateOf<List<String>?>(
                                                null
                                            )
                                        }

                                        showUrlWarning?.also {
                                            UrlWarningDialog(
                                                viewModel = mainViewModel,
                                                urls = it,
                                                onDismiss = { allow, remember ->
                                                    mainViewModel.postEvent(
                                                        MainViewModel.LocalEvents.UrlWarningResponse(
                                                            allow = allow,
                                                            remember = remember
                                                        )
                                                    )
                                                    showUrlWarning = null
                                                })
                                        }

                                        // Handle side effects from MainViewModel like navigating from handled intent
                                        HandleSideEffect(mainViewModel) {
                                            if (it is SideEffects.UrlWarning) {
                                                showUrlWarning = it.urls
                                            } else if (it is SideEffects.TorWarning) {
                                                dialogState.openDialog(
                                                    OpenDialogData(
                                                        title = StringHolder.create(Res.string.id_warning),
                                                        message = StringHolder.create(Res.string.id_do_you_want_to_enable_tor),
                                                        primaryText = getString(Res.string.id_enable),
                                                        onPrimary = {
                                                            mainViewModel.postEvent(
                                                                MainViewModel.LocalEvents.TorWarningResponse(
                                                                    enable = true
                                                                )
                                                            )
                                                        }, onSecondary = {
                                                            mainViewModel.postEvent(
                                                                MainViewModel.LocalEvents.TorWarningResponse(
                                                                    enable = false
                                                                )
                                                            )
                                                        }, onDismiss = {
                                                            mainViewModel.postEvent(
                                                                MainViewModel.LocalEvents.TorWarningResponse(
                                                                    enable = false
                                                                )
                                                            )
                                                        }
                                                    )
                                                )
                                            }
                                        }

                                        FadeSlideTransition(navigator = navigator, content = {
                                            Box(
                                                modifier = Modifier.ifTrue(it !is PromoScreen) {
                                                    padding(innerPadding)
                                                }
                                            ) {
                                                // Allow PromoScreen to cover the whole screen
                                                if (it is PromoScreen) {
                                                    it.Content(innerPadding)
                                                } else {
                                                    it.Content()
                                                }
                                            }
                                        })
                                    },
                                )

                                DialogHost(state = dialogState)
                            }
                        }
                    }
                }
            }

            CompositionLocalProvider(
                LocalBiometricState provides biometricsState
            ) {
                AnimatedVisibility(
                    visible = isLocked,
                    enter = EnterTransition.None,
                    exit = fadeOut()
                ) {
                    LockScreen {
                        mainViewModel.unlock()
                    }
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

@OptIn(ExperimentalCoilApi::class)
@Composable
fun GreenPreview(content: @Composable () -> Unit) {
    // startKoin only once
    KoinPlatformTools.defaultContext().getOrNull() ?: startKoin {
        modules(module {
            single {
                NoKeystore()
            }
            single {
                AppInfo(
                    userAgent = "GreenAndroidPreview",
                    version = "1.0.0-preview",
                    isDebug = true,
                    isDevelopment = true
                )
            }
            single<BluetoothManager?> {
                null
            }
        })
    }

    val dialogState = remember { DialogState() }
//    val platformManager = rememberPlatformManager()

    // Coil preview faker
    val previewHandler = AsyncImagePreviewHandler {
        FakeImage(color = 0xFFFF00)
    }

    GreenChrome()
    GreenTheme {
        CompositionLocalProvider(
            LocalDialog provides dialogState,
//            LocalPlatformManager provides platformManager,
            LocalAsyncImagePreviewHandler provides previewHandler
        ) {
            BottomSheetNavigatorM3 {
                DialogHost(state = dialogState)
                Scaffold { content() }
            }
        }
    }
}
