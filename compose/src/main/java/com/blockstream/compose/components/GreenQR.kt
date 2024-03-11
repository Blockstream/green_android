package com.blockstream.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_surfaceTint
import com.blockstream.compose.utils.QrEncoder
import com.lightspark.composeqr.QrCodeView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GreenQR(
    modifier: Modifier = Modifier,
    data: String?,
    isVisible: Boolean = true,
    visibilityClick: () -> Unit = {}
) {
    val qrEncoder = remember { QrEncoder() }
    var isFullscreen by remember { mutableStateOf(false) }
    val isVisibleAndNotBlank = isVisible && data.isNotBlank()

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
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clickable { isFullscreen = false }
                    ) {
                        QrCodeView(
                            data = data ?: "",
                            encoder = qrEncoder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(24.dp)
                        )
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
                    .widthIn(100.dp, 300.dp)
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = {
                            if (isVisibleAndNotBlank) {
                                isFullscreen = true
                            } else {
                                visibilityClick()
                            }
                        },
                        onLongClick = {
                            if (isVisibleAndNotBlank) {
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
                        QrCodeView(
                            data = data ?: "",
                            encoder = qrEncoder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(18.dp)
                        )
                    } else if (isVisible) {
                        Image(
                            painter = painterResource(id = R.drawable.qr_code),
                            contentDescription = "QR",
                            colorFilter = ColorFilter.tint(green),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        IconButton(
                            text = stringResource(id = R.string.id_show_qr_code),
                            icon = painterResource(id = R.drawable.eye),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            visibilityClick()
                        }
                    }
                }
            }
        }

        if(isVisibleAndNotBlank) {
            ZoomButton {
                isFullscreen = true
            }
        }
    }
}

@Composable
@Preview
fun GreenQRPreview() {
    GreenTheme {
        GreenColumn(modifier = Modifier.fillMaxWidth()) {

//            GreenQR(
//                data = "",
//                modifier = Modifier.weight(1f)
//            )

            GreenQR(
                data = "chalk verb patch cube sell west penalty fish park worry tribe tourist chalk verb patch cube sell west penalty fish park worry tribe tourist",
                modifier = Modifier.fillMaxWidth()
            )

//            GreenQR(
//                data = "chalk verb patch cube sell west penalty fish park worry tribe tourist chalk verb patch cube sell west penalty fish park worry tribe tourist",
//                isVisible = false,
//                modifier = Modifier.weight(1f)
//            )
        }
    }
}