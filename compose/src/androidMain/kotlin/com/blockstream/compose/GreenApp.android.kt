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
import androidx.fragment.app.Fragment
import com.blockstream.common.data.AppInfo
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.compatTestTagsAsResourceId
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

@Composable
fun Fragment.AppFragmentBridge(content: @Composable () -> Unit) {
    val dialogState = remember { DialogState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val platformManager = rememberPlatformManager()

    GreenTheme {
        CompositionLocalProvider(
            LocalDialog provides dialogState,
            LocalSnackbar provides snackbarHostState,
            LocalPlatformManager provides platformManager,
            LocalActivity provides activity,
        ) {
            BottomSheetNavigatorM3 {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    content = { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .compatTestTagsAsResourceId(), // Enable configuration toggle to map testTags to resource-id.
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
