package com.blockstream.compose.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.blockstream.data.extensions.logException
import com.blockstream.data.platformFileSystem
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.OpenBrowserType
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import okio.Source
import java.awt.Desktop
import java.io.File
import java.net.URI

@Composable
actual fun rememberPlatformManager(): PlatformManager = PlatformManager()

actual object StateKeeperFactory {
    actual fun stateKeeper(): StateKeeper = StateKeeperDispatcher()
}

@Composable
actual fun rememberStateKeeperFactory(): StateKeeperFactory = StateKeeperFactory

@Composable
actual fun askForNotificationPermissions(viewModel: GreenViewModel) {

}

actual class PlatformManager {
    actual fun openToast(content: String): Boolean {
        return false
    }

    actual fun openBrowser(url: String, type: OpenBrowserType) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    }

    actual fun copyToClipboard(content: String, label: String?, isSensitive: Boolean): Boolean {
        return false
    }

    internal actual fun getClipboard(): String? {
        return null
    }

    actual fun clearClipboard() {
    }

    actual suspend fun shareText(content: String) {
    }

    actual suspend fun shareFile(path: String, file: PlatformFile?) {
    }

    actual fun hasFlash(): Boolean = false

    actual suspend fun scanQrFromImage(file: String): String? {
        return null
    }

    actual suspend fun scanQrFromByteArray(data: ByteArray): String? {
        return "not yet implemented"
    }

    actual suspend fun processQr(data: ByteArray, text: String): ByteArray {
        // TODO
        return data
    }

    actual fun fileToSource(file: String): Source? {
        return platformFileSystem().source(file.toPath())
    }

    actual fun enableBluetooth() {
    }

    actual fun enableLocationService() {
    }

    actual fun openBluetoothSettings() {
    }

    actual fun setSecureScreen(isSecure: Boolean) {
    }
}

@Composable
actual fun rememberImagePicker(
    scope: CoroutineScope,
    onResult: suspend (ByteArray) -> Unit
): ImagePickerLauncher {
    var showFilePicker by remember { mutableStateOf(false) }

    FilePicker(
        show = showFilePicker,
        fileExtensions = listOf("jpg", "jpeg", "png", "heic")
    ) { platformFile ->
        showFilePicker = false
        platformFile?.path?.also {
            scope.launch(context = logException()) {
                onResult(
                    withContext(context = Dispatchers.IO) { File(it).inputStream().readBytes() }
                )
            }
        }
    }

    return remember {
        ImagePickerLauncher {
            showFilePicker = true
        }
    }
}

actual class ImagePickerLauncher actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch.invoke()
    }
}

@Composable
actual fun askForBluetoothPermissions(viewModel: GreenViewModel, fn: () -> Unit) {
}