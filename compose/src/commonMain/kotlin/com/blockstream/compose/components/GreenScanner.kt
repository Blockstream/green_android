package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_scan_from_image
import blockstream_green.common.generated.resources.lightning
import blockstream_green.common.generated.resources.lightning_slash
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.abstract.AbstractScannerViewModel
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.extensions.pxToDp
import com.blockstream.compose.managers.rememberImagePicker
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.getScreenSizeInfo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenScanner(
    modifier: Modifier = Modifier,
    isDecodeContinuous: Boolean = true,
    showScanFromImage: Boolean = true,
    viewModel: AbstractScannerViewModel
) {
    val screenSizeInfo = getScreenSizeInfo()
    val platformManager = rememberPlatformManager()

    val height = (screenSizeInfo.heightPx * 0.70).toInt().pxToDp()

    var isFlashOn by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .height(height)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .align(Alignment.BottomCenter),
        ) {
            CameraView(
                modifier = Modifier.fillMaxSize(),
                isFlashOn = isFlashOn,
                isDecodeContinuous = isDecodeContinuous,
                showScanFromImage = showScanFromImage,
                viewModel = viewModel
            )

            val progress by viewModel.progress.collectAsStateWithLifecycle()

            AnimatedNullableVisibility(progress, modifier = Modifier.align(Alignment.BottomCenter)) {
                LinearProgressIndicator(progress = {
                    it / 100f
                }, modifier = Modifier.fillMaxWidth().height(4.dp))
            }
        }

        val hasFlash = remember {
            platformManager.hasFlash()
        }

        if (hasFlash) {
            Image(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .clickable {
                        isFlashOn = !isFlashOn
                    }
                    .padding(8.dp),
                painter = painterResource(if (isFlashOn) Res.drawable.lightning_slash else Res.drawable.lightning),
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

            val imagePicker = rememberImagePicker(scope = scope) {

                val result = platformManager.scanQrFromByteArray(it)

                if (result != null) {
                    viewModel.postEvent(Events.SetBarcodeScannerResult(result))
                } else {
                    dialog.openErrorDialog(Exception("id_could_not_recognized_qr_code"))
                }
            }

            if (showScanFromImage) {
                GreenButton(
                    text = stringResource(Res.string.id_scan_from_image),
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.SMALL,
                ) {
                    imagePicker.launch()
                }
            }
        }
    }
}