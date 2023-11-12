package com.blockstream.compose.sheets

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.koin.getScreenModel
import com.blockstream.common.models.camera.CameraViewModel
import com.blockstream.common.models.camera.CameraViewModelAbstract
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.utils.BarcodeAnalyser
import com.blockstream.compose.views.GreenBottomSheet
import kotlinx.parcelize.Parcelize
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage

@OptIn(ExperimentalMaterial3Api::class)
@Parcelize
class CameraBottomSheet : BottomScreen() {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<CameraViewModel>()
        CameraBottomSheet(viewModel = viewModel, onDismissRequest = onDismissRequest())
    }
}

@androidx.camera.core.ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraBottomSheet(
    viewModel: CameraViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(id = R.string.id_scan_qr_code),
//        subtitle = viewModel.greenWallet.name,
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = onDismissRequest) {

        PreviewViewComposable()
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
fun PreviewViewComposable() {
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics

    AndroidView({ context ->
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val previewView = PreviewView(context).also {
            it.scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyser{
                        Toast.makeText(context, "Barcode found", Toast.LENGTH_SHORT).show()
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    context as ComponentActivity, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e("DEBUG", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
        previewView
    },

        modifier = Modifier
            .height(600.dp)
//            .size(height = (displayMetrics.heightPixels / 2).dp)
            .clip(RoundedCornerShape(8.dp))
            .fillMaxWidth())

}



@androidx.camera.core.ExperimentalGetImage

@ExperimentalMaterial3Api
@Composable
@androidx.compose.ui.tooling.preview.Preview
fun CameraBottomSheetPreview() {
    GreenTheme {
        GreenColumn {
            PreviewViewComposable()
//            var showBottomSheet by remember { mutableStateOf(true) }
//
//            GreenButton(text = "Show BottomSheet") {
//                showBottomSheet = true
//            }
//
//            Text("WalletRenameBottomSheet")
//
//            if(showBottomSheet) {
//                CameraBottomSheet(
//                    viewModel = CameraViewModelPreview.preview(),
//                    onDismissRequest = {
//                        showBottomSheet = false
//                    }
//                )
//            }
        }
    }
}