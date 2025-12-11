package com.blockstream.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.id_show_qr_code
import blockstream_green.common.generated.resources.qr_code
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_surfaceTint
import com.blockstream.compose.utils.ifTrue
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GreenQR(
    modifier: Modifier = Modifier,
    data: String?,
    isVisible: Boolean = true,
    isJadeQR: Boolean = false,
    onQrClick: (() -> Unit)? = null,
    visibilityClick: () -> Unit = {}
) {
    var isFullscreen by remember { mutableStateOf(false) }
    val isVisibleAndNotBlank = isVisible && data.isNotBlank()
    val qrPadding = if (isJadeQR) 28.dp else 18.dp
    val qrCodePainter = rememberQrCodePainter(
        data = data ?: "",
        errorCorrectionLevel = if (isJadeQR) QrErrorCorrectionLevel.Low else QrErrorCorrectionLevel.Auto
    )

    Column(
        modifier = Modifier
            .then(modifier),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box {
            if (isFullscreen) {
                Dialog(
                    onDismissRequest = { isFullscreen = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                    )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { isFullscreen = false }
                            .ifTrue(isJadeQR) {
                                it.background(Color.White)
                            }
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .aspectRatio(1f)
                                .fillMaxSize()

                        ) {
                            Image(
                                painter = qrCodePainter,
                                contentDescription = "QR",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(qrPadding)
                            )
                        }
                    }
                }
            }

            val color = if (isVisibleAndNotBlank) Color.White else md_theme_surfaceTint
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = color,
                    contentColor = color
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(100.dp, 400.dp)
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = {
                            if (isVisibleAndNotBlank) {
                                if (onQrClick != null) {
                                    onQrClick()
                                } else if (!isJadeQR) {
                                    isFullscreen = true
                                }
                            } else {
                                visibilityClick()
                            }
                        },
                        onLongClick = {
                            if (isVisibleAndNotBlank && !isJadeQR) {
                                isFullscreen = true
                            } else {
                                visibilityClick()
                            }
                        },
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {

                    if (isVisibleAndNotBlank) {
                        Image(
                            painter = qrCodePainter,
                            contentDescription = "QR",
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(if (isJadeQR) 0.dp else qrPadding)
                        )
                    } else if (isVisible) {
                        Image(
                            painter = painterResource(Res.drawable.qr_code),
                            contentDescription = "QR",
                            colorFilter = ColorFilter.tint(green),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        GreenIconButton(
                            text = stringResource(Res.string.id_show_qr_code),
                            icon = painterResource(Res.drawable.eye),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            visibilityClick()
                        }
                    }
                }
            }
        }

        if (isVisibleAndNotBlank && !isJadeQR) {
            ZoomButton {
                isFullscreen = true
            }
        }
    }
}
