package com.blockstream.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.blockstream.common.data.AppInfo
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.theme.GreenTheme
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

@Composable
fun AppFragmentBridge(content: @Composable () -> Unit) {
    val dialogState = remember { DialogState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val platformManager = rememberPlatformManager()

    GreenTheme {
        CompositionLocalProvider(
            androidx.lifecycle.compose.LocalLifecycleOwner provides androidx.compose.ui.platform.LocalLifecycleOwner.current, // Until Compose 1.7.0 is released // https://stackoverflow.com/questions/78490378/java-lang-illegalstateexception-compositionlocal-locallifecycleowner-not-presen/78490602#78490602
            LocalDialog provides dialogState,
            LocalSnackbar provides snackbarHostState,
            LocalPlatformManager provides platformManager
        ) {
            BottomSheetNavigatorM3 {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    content = { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding),
                        ) {
                            content()
                        }
                    },
                )
                DialogHost(state = dialogState)
            }
        }
    }
}

@Composable
fun GreenAndroidPreview(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dialogState = remember { DialogState() }
    val platformManager = rememberPlatformManager()

    // startKoin only once
    KoinPlatformTools.defaultContext().getOrNull() ?: startKoin {
        modules(module {
            single { context }
            single {
                AndroidKeystore(androidContext())
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

    GreenTheme {
        CompositionLocalProvider(
            androidx.lifecycle.compose.LocalLifecycleOwner provides androidx.compose.ui.platform.LocalLifecycleOwner.current, // Until Compose 1.7.0 is released // https://stackoverflow.com/questions/78490378/java-lang-illegalstateexception-compositionlocal-locallifecycleowner-not-presen/78490602#78490602
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
