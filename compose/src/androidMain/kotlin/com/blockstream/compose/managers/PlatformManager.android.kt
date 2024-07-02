package com.blockstream.compose.managers

import android.Manifest
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Message
import android.os.PersistableBundle
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.savedstate.SavedStateRegistryOwner
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_share
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.stateKeeper
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.models.GreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.journeyapps.barcodescanner.MixedDecoder
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Source
import okio.source
import org.jetbrains.compose.resources.getString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberPlatformManager(): PlatformManager {
    val context = LocalContext.current

    return remember {
        PlatformManager(context)
    }
}
actual class StateKeeperFactory(val savedStateRegistryOwner: SavedStateRegistryOwner) {
    actual fun stateKeeper(): StateKeeper {
        return savedStateRegistryOwner.stateKeeper()
    }
}

@Composable
actual fun rememberStateKeeperFactory(): StateKeeperFactory {
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    return remember {
        StateKeeperFactory(savedStateRegistryOwner)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun askForNotificationPermissions(viewModel: GreenViewModel) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.postEvent(Events.NotificationPermissionGiven)
            } else {
                // Handle permission denial
            }
        }

        val notificatioPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(notificatioPermissionState) {
            if (!notificatioPermissionState.status.isGranted && notificatioPermissionState.status.shouldShowRationale) {
                // Show rationale if needed
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

actual class PlatformManager(val context: Context) {

    actual fun openToast(content: String): Boolean {
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show()
        return true
    }

    actual fun openBrowser(url: String) {
        try {
            val builder = CustomTabsIntent.Builder()
            builder.setShowTitle(true)
            builder.setUrlBarHidingEnabled(false)
            builder.setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
//                    .setToolbarColor(ContextCompat.getColor(context, R.color.brand_surface))
//                    .setNavigationBarColor(ContextCompat.getColor(context, R.color.brand_surface))
//                    .setNavigationBarDividerColor(
//                        ContextCompat.getColor(
//                            context,
//                            R.color.brand_green
//                        )
//                    )
                    .build()
            )
//            builder.setStartAnimations(context, R.anim.enter_slide_up, R.anim.fade_out)
//            builder.setExitAnimations(context, R.anim.fade_in, R.anim.exit_slide_down)

            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun copyToClipboard(content: String, label: String?, isSensitive: Boolean): Boolean {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
            ClipData.newPlainText(label ?: "Green", content).apply {
                if (isSensitive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        description.extras = PersistableBundle().apply {
                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                        }
                    }
                }
            }
        )

        return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
    }

    internal actual fun getClipboard(): String? {
        return (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
            it.primaryClip?.getItemAt(0)?.text?.toString()
        }
    }

    actual fun clearClipboard() {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.clearPrimaryClip()
            }
        }
    }

    actual suspend fun shareText(content: String) {
        val builder = ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText(content)

        context.startActivity(
            Intent.createChooser(
                builder.intent,
                getString(Res.string.id_share)
            )
        )
    }

    actual suspend fun shareFile(path: String) {
        val fileUri = FileProvider.getUriForFile(
            context,
            context.packageName.toString() + ".provider",
            File(path)
        )

        val builder = ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setStream(fileUri)

        context.startActivity(
            Intent.createChooser(
                builder.intent,
                getString(Res.string.id_share)
            )
        )
    }

    actual fun hasFlash(): Boolean = context.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) ?: false

    actual suspend fun scanQrFromImage(file: String): String? {
        return try {
            withContext(context = Dispatchers.IO + logException()) {
                val uri = Uri.parse(file)
                scanFromBitmap(
                    if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.RGBA_F16, true)
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun scanQrFromByteArray(data: ByteArray): String? {
        return try {
            withContext(context = Dispatchers.IO + logException()) {
                scanFromBitmap(BitmapFactory.decodeByteArray(data, 0, data.size))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scanFromBitmap(image: Bitmap): String? {
        val intArray = IntArray(image.width * image.height)
        image.getPixels(intArray, 0, image.width, 0, 0, image.width, image.height)

        val source = RGBLuminanceSource(image.width, image.height, intArray)
        val reader = MixedDecoder(MultiFormatReader())
        var result = reader.decode(source)
        if (result == null) {
            result = reader.decode(source)
        }
        return result.text
    }

    actual suspend fun processQr(data: ByteArray, text: String): ByteArray {
        return withContext(context = Dispatchers.IO){
            try {
                val extraTextHeight = 80
                val imageSize = 800
                val padding = 16

                val qrCodeScalled = Bitmap.createScaledBitmap(
                    data.toImageBitmap().asAndroidBitmap(),
                    imageSize - (padding * 2),
                    imageSize - (padding * 2),
                    false
                );

                val bitmap = Bitmap.createBitmap(
                    imageSize,
                    imageSize + extraTextHeight,
                    Bitmap.Config.ARGB_8888
                )

                val canvas = Canvas(bitmap)
                canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF) // White
                canvas.drawBitmap(qrCodeScalled, padding.toFloat(), padding.toFloat(), null)


                val textPaint = TextPaint()
                textPaint.isAntiAlias = true
                textPaint.textSize = 18.0f
                textPaint.typeface = Typeface.MONOSPACE

                val staticLayout = StaticLayout.Builder.obtain(
                    text,
                    0,
                    text.length,
                    textPaint,
                    imageSize - (padding * 2)
                )
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .setMaxLines(4)
                    .setEllipsize(TextUtils.TruncateAt.MIDDLE)
                    .build()

                canvas.translate(padding.toFloat(), imageSize.toFloat())
                staticLayout.draw(canvas)


                val stream = ByteArrayOutputStream()

                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)

                stream.toByteArray()
            } catch (e: Exception) {
                e.printStackTrace()
                data
            }
        }
    }

    actual fun fileToSource(file: String): Source? {
        return try {
            context.contentResolver.openInputStream(Uri.parse(file))?.source()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
