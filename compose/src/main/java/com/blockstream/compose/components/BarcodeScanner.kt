package com.blockstream.compose.components

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.models.abstract.AbstractScannerViewModel
import com.blockstream.common.models.camera.CameraViewModelPreview
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.R
import com.blockstream.compose.android.views.ViewFinderView
import com.blockstream.compose.extensions.pxToDp
import com.blockstream.compose.theme.GreenThemePreview
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.MixedDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_FRAME_THICKNESS_DP = 3f
private const val DEFAULT_MASK_COLOR = 0x22000000
private const val DEFAULT_FRAME_COLOR = android.graphics.Color.WHITE
private const val DEFAULT_FRAME_CORNER_SIZE_DP = 50f
private const val DEFAULT_FRAME_SIZE = 0.65f

@Composable
fun BarcodeScanner(
    modifier: Modifier = Modifier,
    isDecodeContinuous: Boolean = true,
    showScanFromImage: Boolean = true,
    viewModel: AbstractScannerViewModel
) {
    var isTorchOn by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val height = (displayMetrics.heightPixels * 0.70).toInt().pxToDp()

    Box(modifier = modifier) {
        if (LocalInspectionMode.current) {
            Box(
                modifier = Modifier
                    .height(height)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.BottomCenter)
                    .background(Color.LightGray)
            )
        } else {
            var captureManager by remember {
                mutableStateOf<CaptureManager?>(null)
            }

            AndroidView(
                modifier = Modifier
                    .height(height)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.BottomCenter),
                factory = { context ->
                    LayoutInflater.from(context).inflate(R.layout.camera, null).apply {

                        val decoratedBarcode =
                            findViewById<DecoratedBarcodeView>(R.id.decorated_barcode)
                        val viewFinder = findViewById<ViewFinderView>(R.id.view_finder)

                        viewFinder.maskColor = DEFAULT_MASK_COLOR
                        viewFinder.frameColor = DEFAULT_FRAME_COLOR
                        viewFinder.frameThickness = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            DEFAULT_FRAME_THICKNESS_DP,
                            resources.displayMetrics
                        ).toInt()
                        viewFinder.frameCornersSize = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            DEFAULT_FRAME_CORNER_SIZE_DP,
                            resources.displayMetrics
                        ).toInt()
                        viewFinder.frameSize = DEFAULT_FRAME_SIZE

                        decoratedBarcode.apply {
                            this.viewFinder.isVisible = false
                            this.statusView.isVisible = false
                            // Scan black/white or inverted
                            this.barcodeView.decoderFactory =
                                DefaultDecoderFactory(null, null, null, Intents.Scan.MIXED_SCAN)
                        }

                        decoratedBarcode.cameraSettings.apply {
                            isMeteringEnabled = true
                            isExposureEnabled = true
                            isContinuousFocusEnabled = true
                        }

                        val callback = BarcodeCallback { result ->
                            viewModel.postEvent(Events.SetBarcodeScannerResult(result.text))
                        }

                        if (isDecodeContinuous) {
                            decoratedBarcode.decodeContinuous(callback)
                        } else {
                            decoratedBarcode.decodeSingle(callback)
                        }

                        captureManager =
                            CaptureManager(context as Activity, decoratedBarcode).also {
                                it.setShowMissingCameraPermissionDialog(true)
                                it.onResume()
                            }

                    }
                }, update = { view ->
                    val decoratedBarcode =
                        view.findViewById<DecoratedBarcodeView>(R.id.decorated_barcode)

                    if (isTorchOn) {
                        decoratedBarcode.setTorchOn()
                    } else {
                        decoratedBarcode.setTorchOff()
                    }
                })

            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                // Create an observer that triggers our remembered callbacks
                // for sending analytics events
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            captureManager?.onResume()
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            captureManager?.onPause()
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            captureManager?.onDestroy()
                        }

                        else -> { }
                    }
                }

                // Add the observer to the lifecycle
                lifecycleOwner.lifecycle.addObserver(observer)

                // When the effect leaves the Composition, remove the observer
                onDispose {
                    captureManager?.onPause()
                    captureManager?.onDestroy()
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }


        val hasFlash = remember {
            context.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) ?: false
        }
        if(hasFlash) {
            Image(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .clickable {
                        isTorchOn = !isTorchOn
                    }
                    .padding(8.dp),
                painter = painterResource(id = if (isTorchOn) R.drawable.lightning_slash else R.drawable.lightning),
                contentDescription = "Flash"
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
        ) {

            val dialog = LocalDialog.current

            val scope = rememberCoroutineScope()

            val openGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.also {
                    scope.launch(context = logException()) {

                        val result = withContext(context = Dispatchers.IO + logException()){
                            try {
                                val image = if (Build.VERSION.SDK_INT < 28) {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                } else {
                                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                                    ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.RGBA_F16, true)
                                }

                                val intArray = IntArray(image.width * image.height)
                                image.getPixels(intArray, 0, image.width, 0, 0, image.width, image.height)

                                val source = RGBLuminanceSource(image.width, image.height, intArray)
                                val reader = MixedDecoder(MultiFormatReader())
                                var result = reader.decode(source)
                                if (result == null) {
                                    result = reader.decode(source)
                                }
                                result
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                        }

                        if (result != null) {
                            viewModel.postEvent(Events.SetBarcodeScannerResult(result.text))
                        } else {
                            dialog.openErrorDialog(Exception("id_could_not_recognized_qr_code"))
                        }
                    }
                }
            }

            if (showScanFromImage) {
                GreenButton(
                    text = stringResource(id = R.string.id_scan_from_image),
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL,
                ) {
                    openGallery.launch("image/*")
                }
            }
        }
    }
}

@Composable
@Preview
fun BarcodeScannerPreview() {
    GreenThemePreview {
        GreenColumn {
            BarcodeScanner(isDecodeContinuous = false, viewModel = CameraViewModelPreview.preview(),
                modifier = Modifier.aspectRatio(1f))
        }
    }
}