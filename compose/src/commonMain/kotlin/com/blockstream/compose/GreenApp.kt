@file:OptIn(ExperimentalSettingsApi::class)

package com.blockstream.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_you_have_clicked_a_payment_uri
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.test.FakeImage
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.models.MainViewModel
import com.blockstream.compose.navigation.AppScaffold
import com.blockstream.compose.navigation.LocalNavData
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.Router
import com.blockstream.compose.navigation.bottomsheet.ModalBottomSheetLayout
import com.blockstream.compose.navigation.bottomsheet.rememberBottomSheetNavigator
import com.blockstream.compose.navigation.navigate
import com.blockstream.compose.screens.LockScreen
import com.blockstream.compose.sideeffects.BiometricsState
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.sideeffects.rememberBiometricsState
import com.blockstream.compose.theme.GreenChrome
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.data.config.AppInfo
import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.crypto.NoKeystore
import com.blockstream.data.managers.BluetoothManager
import com.blockstream.data.managers.DeviceManager
import com.blockstream.data.managers.LifecycleManager
import com.blockstream.data.managers.SettingsManager
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

val LocalAppInfo: ProvidableCompositionLocal<AppInfo> =
    staticCompositionLocalOf { error("LocalAppInfo not initialized") }
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }
val LocalAppCoroutine =
    staticCompositionLocalOf { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
val LocalDialog = staticCompositionLocalOf { DialogState() }
val LocalActivity: ProvidableCompositionLocal<Any?> = compositionLocalOf { null }
val LocalPreview = compositionLocalOf { false }
val LocalBiometricState: ProvidableCompositionLocal<BiometricsState?> = compositionLocalOf { null }

private val SecureScreens = listOf(
    NavigateDestinations.RecoveryIntro::class,
    NavigateDestinations.RecoveryCheck::class,
    NavigateDestinations.RecoveryWords::class,
    NavigateDestinations.RecoveryPhrase::class,
    NavigateDestinations.Login::class,
    NavigateDestinations.SetPin::class,
    NavigateDestinations.ChangePin::class,
)

@Composable
fun GreenApp(mainViewModel: MainViewModel, modifier: Modifier = Modifier) {
    val platformManager = rememberPlatformManager()
    val appInfo = koinInject<AppInfo>()
    val deviceManager = koinInject<DeviceManager>()
    val settings by mainViewModel.settingsManager.appSettingsStateFlow.collectAsStateWithLifecycle()

    val snackbarHostState = LocalSnackbar.current
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    val backstackEntry by navController.currentBackStackEntryAsState()

    val navData by LocalNavData.current.data

    navController.addOnDestinationChangedListener { _, destination, _ ->
        // Listen only to ComposeNavigator events
        if (destination.navigatorName != "composable") return@addOnDestinationChangedListener

        if (destination.let { it.hasRoute<NavigateDestinations.DeviceList>() || it.hasRoute<NavigateDestinations.DeviceScan>() }) {
            deviceManager.startDeviceDiscovery()
        } else {
            deviceManager.stopDeviceDiscovery()
        }

        // If enhancedPrivacy is turned off, secure only specific screens
        if (!settings.enhancedPrivacy) {
            // Set secure screen based on current screen
            platformManager.setSecureScreen(appInfo.isProduction && SecureScreens.any {
                destination.hasRoute(it)
            })
        }
    }

    val isLocked by mainViewModel.lockScreen.collectAsStateWithLifecycle()

    // Set secure screen based on enhanced privacy setting
    LaunchedEffect(settings.enhancedPrivacy) {
        // Skip changing secure screen if we are on a secure fragment
        if (settings.enhancedPrivacy || !SecureScreens.any {
                backstackEntry?.destination?.hasRoute(
                    it
                ) == true
            }
        ) {
            platformManager.setSecureScreen(settings.enhancedPrivacy)
        }
    }

    CompositionLocalProvider(
        LocalPlatformManager provides platformManager,
        LocalAppInfo provides appInfo,
        LocalNavigator provides navController
    ) {
        val biometricsState = rememberBiometricsState()

        val pendingUri by mainViewModel.sessionManager.pendingUri.collectAsStateWithLifecycle()

        LaunchedEffect(pendingUri) {
            pendingUri?.also {
                if (backstackEntry?.destination?.hasRoute<NavigateDestinations.Home>() == true) {
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
            LocalBiometricState provides biometricsState
        ) {
            Box(modifier = modifier) {

                AppScaffold(
                    navData = navData,
                    snackbarHostState = snackbarHostState,
                    mainViewModel = mainViewModel,
                    navigate = {
                        navigate(navController, it)
                    },
                    goBack = {
                        navController.navigateUp()
                    }
                ) { innerPadding ->

                    val dialogState = LocalDialog.current

                    // Bottom Sheets
                    ModalBottomSheetLayout(bottomSheetNavigator = bottomSheetNavigator)

                    // Generic Dialogs
                    DialogHost(state = dialogState)

                    Router(
                        mainViewModel = mainViewModel,
                        navController = navController,
                        innerPadding = innerPadding,
                        startDestination = NavigateDestinations.Home
                    )
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

@OptIn(ExperimentalCoilApi::class)
@Composable
fun GreenPreview(content: @Composable () -> Unit) {
    // startKoin only once
    KoinPlatformTools.defaultContext().getOrNull() ?: startKoin {
        modules(module {
            single<GreenKeystore> { NoKeystore() }
            single<BluetoothManager?> { null }
            single<ObservableSettings> { Settings().makeObservable() }
            single { LifecycleManager(get(), get()) }
            single {
                SettingsManager(
                    settings = get(),
                    analyticsFeatureEnabled = true,
                    lightningFeatureEnabled = true,
                    storeRateEnabled = true
                )
            }
            single {
                AppInfo(
                    userAgent = "GreenAndroidPreview",
                    version = "1.0.0-preview",
                    isDebug = true,
                    isDevelopment = true
                )
            }
        })
    }

    val dialogState = remember { DialogState() }
    val platformManager = rememberPlatformManager()
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    // Coil preview faker
    val previewHandler = AsyncImagePreviewHandler {
        FakeImage(color = 0xFFFF00)
    }

    GreenChrome()
    GreenTheme {
        CompositionLocalProvider(
            LocalDialog provides dialogState,
            LocalNavigator provides navController,
            LocalPlatformManager provides platformManager,
            LocalAsyncImagePreviewHandler provides previewHandler
        ) {
            // Bottom Sheets
            ModalBottomSheetLayout(bottomSheetNavigator = bottomSheetNavigator)
            DialogHost(state = dialogState)
            Scaffold { innerPadding ->

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(innerPadding)
                        .padding(innerPadding)
                ) {
                    content()
                }
            }
        }
    }
}
