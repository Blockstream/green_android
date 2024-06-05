package com.blockstream.compose.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.blockstream.common.models.GreenViewModel
import kotlinx.coroutines.CoroutineScope
import okio.Source

val LocalPlatformManager: ProvidableCompositionLocal<PlatformManager> =
    staticCompositionLocalOf { error("PlatformManager not initialized") }

expect class ImagePickerLauncher(
    onLaunch: () -> Unit,
) {
    fun launch()
}


@Composable
expect fun rememberPlatformManager(): PlatformManager

@Composable
expect fun rememberStateKeeperFactory(): StateKeeperFactory

expect class StateKeeperFactory {
    fun stateKeeper(): StateKeeper
}

@Composable
expect fun rememberImagePicker(scope: CoroutineScope, onResult: suspend (ByteArray) -> Unit): ImagePickerLauncher

@Composable
expect fun askForNotificationPermissions(viewModel: GreenViewModel)

expect class PlatformManager {
    fun openBrowser(url: String)

    fun copyToClipboard(content: String, label: String? = null)
    internal fun getClipboard(): String?
    fun clearClipboard()

    suspend fun shareText(content: String)
    suspend fun shareFile(path: String)

    fun hasFlash(): Boolean

    suspend fun scanQrFromImage(file: String): String?
    suspend fun scanQrFromByteArray(data: ByteArray): String?

    suspend fun processQr(data: ByteArray, text: String): ByteArray

    fun fileToSource(file: String): Source?
}

fun PlatformManager.getClipboard(clearClipboard: Boolean = false): String? {
    return getClipboard().also {
        if (clearClipboard) {
            clearClipboard()
        }
    }
}