package com.blockstream.compose.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.OpenBrowserType
import io.github.vinceglb.filekit.PlatformFile
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

@Composable
expect fun askForBluetoothPermissions(viewModel: GreenViewModel, fn: () -> Unit)

expect class PlatformManager {
    fun openBrowser(url: String, type: OpenBrowserType)

    fun openToast(content: String): Boolean

    fun copyToClipboard(content: String, label: String? = null, isSensitive: Boolean = false): Boolean
    internal fun getClipboard(): String?
    fun clearClipboard()

    suspend fun shareText(content: String)
    suspend fun shareFile(path: String? = null, file: PlatformFile? = null)

    fun hasFlash(): Boolean

    suspend fun scanQrFromImage(file: String): String?
    suspend fun scanQrFromByteArray(data: ByteArray): String?

    suspend fun processQr(data: ByteArray, text: String): ByteArray

    fun fileToSource(file: String): Source?

    fun enableBluetooth()
    fun enableLocationService()
    fun openBluetoothSettings()

    fun setSecureScreen(isSecure: Boolean)
}

fun PlatformManager.getClipboard(clearClipboard: Boolean = false): String? {
    return getClipboard().also {
        if (clearClipboard) {
            clearClipboard()
        }
    }
}