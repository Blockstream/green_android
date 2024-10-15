package com.blockstream.compose.managers

import androidx.compose.runtime.Composable
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.blockstream.common.extensions.logException
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.platformFileSystem
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import okio.Source
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorAccuracy
import platform.CoreImage.CIDetectorAccuracyHigh
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberPlatformManager(): PlatformManager {
    return PlatformManager(UIApplication.sharedApplication())
}

actual object StateKeeperFactory {
    actual fun stateKeeper(): StateKeeper = StateKeeperDispatcher()
}

@Composable
actual fun rememberStateKeeperFactory(): StateKeeperFactory = StateKeeperFactory


@Composable
actual fun askForNotificationPermissions(viewModel: GreenViewModel) {

}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformManager(val application: UIApplication) {
    actual fun openToast(content: String): Boolean {
        return false
    }

    actual fun openBrowser(url: String, openSystemBrowser: Boolean) {
        NSURL(string = url).takeIf { application.canOpenURL(it) }?.also {
            application.openURL(it)
        }
    }

    actual fun copyToClipboard(content: String, label: String?, isSensitive: Boolean): Boolean {
        UIPasteboard.generalPasteboard().string = content
        return false
    }

    internal actual fun getClipboard(): String? {
        return UIPasteboard.generalPasteboard().string
    }

    actual fun clearClipboard() {
        UIPasteboard.generalPasteboard().string = null
    }

    actual suspend fun shareText(content: String) {

    }

    actual suspend fun shareFile(path: String) {

    }

    actual fun hasFlash(): Boolean = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.hasTorch == true

    actual suspend fun scanQrFromImage(file: String): String? {
        return try {
            withContext(context = Dispatchers.IO + logException()) {
                UIImage.imageWithContentsOfFile(file)?.CGImage?.let { CIImage.imageWithCGImage(it) }?.let { ciImage ->
                    CIDetector.detectorOfType(
                        type = CIDetectorTypeQRCode,
                        context = null,
                        options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh)
                    )?.featuresInImage(ciImage)?.first { it is CIQRCodeFeature }
                        ?.let { (it as? CIQRCodeFeature)?.messageString }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun scanQrFromByteArray(data: ByteArray): String? {
        return try {
            withContext(context = Dispatchers.IO + logException()) {
                memScoped {
                    NSData.create(bytes = allocArrayOf(data), length = data.size.toULong())
                }.let { nsData ->
                    UIImage.imageWithData(nsData)?.let {
                        scanQrFromUImage(uiImage = it)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scanQrFromUImage(uiImage: UIImage): String? {
        return uiImage.CGImage?.let { CIImage.imageWithCGImage(it) }?.let { ciImage ->
            CIDetector.detectorOfType(
                type = CIDetectorTypeQRCode,
                context = null,
                options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh)
            )?.featuresInImage(ciImage)?.firstOrNull { it is CIQRCodeFeature }
                ?.let { (it as? CIQRCodeFeature)?.messageString }
        }
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
    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let {
                scope.launch {
                    onResult.invoke(it)
                }
            }
        }
    )

    return ImagePickerLauncher {
        imagePicker.launch()
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