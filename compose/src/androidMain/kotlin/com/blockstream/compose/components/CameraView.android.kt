package com.blockstream.compose.components

import android.util.TypedValue
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import co.touchlab.kermit.Logger
import com.blockstream.common.events.Events
import com.blockstream.common.models.abstract.AbstractScannerViewModel
import com.blockstream.common.models.camera.CameraViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.LocalActivity
import com.blockstream.compose.R
import com.blockstream.compose.android.views.ViewFinderView
import com.blockstream.ui.components.GreenColumn
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

private const val DEFAULT_FRAME_THICKNESS_DP = 3f
private const val DEFAULT_MASK_COLOR = 0x22000000
private const val DEFAULT_FRAME_COLOR = android.graphics.Color.WHITE
private const val DEFAULT_FRAME_CORNER_SIZE_DP = 50f
private const val DEFAULT_FRAME_SIZE = 0.65f

@Composable
actual fun CameraView(
    modifier: Modifier,
    isFlashOn: Boolean,
    isDecodeContinuous: Boolean,
    showScanFromImage: Boolean,
    viewModel: AbstractScannerViewModel
) {

    var captureManager by remember {
        mutableStateOf<CaptureManager?>(null)
    }

    val activity = LocalActivity.current as? FragmentActivity

    if (activity != null) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
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
                        CaptureManager(activity, decoratedBarcode).also {
                            it.setShowMissingCameraPermissionDialog(true)
                        }

                }
            }, update = { view ->
                val decoratedBarcode =
                    view.findViewById<DecoratedBarcodeView>(R.id.decorated_barcode)

                if (isFlashOn) {
                    decoratedBarcode.setTorchOn()
                } else {
                    decoratedBarcode.setTorchOff()
                }
            })
    } else {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Text("Preview Model", modifier = Modifier.align(Alignment.Center))
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.STARTED, Lifecycle.State.RESUMED -> {
                Logger.d { "BarcodeScanner Started/Resumed" }
                captureManager?.onResume()
            }

            Lifecycle.State.DESTROYED -> {
                Logger.d { "BarcodeScanner Destroyed" }
                captureManager?.onPause()
            }

            else -> {}
        }
    }

    DisposableEffect(lifecycleOwner) {
        // When the effect leaves the Composition, remove the observer
        onDispose {
            Logger.d { "BarcodeScanner onDispose" }
            captureManager?.onPause()
            captureManager?.onDestroy()
        }
    }
}

@Composable
@Preview
fun BarcodeScannerPreview() {
    GreenAndroidPreview {
        GreenColumn {
            CameraView(
                isDecodeContinuous = false, viewModel = CameraViewModelPreview.preview(),
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}