@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.screens.lightning

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockstream.common.events.Events
import com.blockstream.common.models.add.ExportLightningKeyViewModelAbstract
import com.blockstream.common.models.add.ExportLightningKeyViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.headlineLarge
import com.blockstream.compose.utils.rememberQrBitmapPainter
import com.blockstream.compose.views.ScreenContainer

@Composable
fun ExportLightningKeyScreen(
    viewModel: ExportLightningKeyViewModelAbstract
) {
    ScreenContainer(viewModel = viewModel) {

        var isFullscreen by remember { mutableStateOf(false) }

        GreenColumn(space = 24, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.id_scan_qr_with_jade), style = headlineLarge)
            Text(
                text = stringResource(id = R.string.id_jade_will_securely_create_and_transfer)
            )

            val qrCode by viewModel.bcurPart.collectAsState()
            val qrPainter = qrCode?.let { rememberQrBitmapPainter(it) }

            if (isFullscreen && qrPainter != null) {
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
                            .padding(8.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clickable { isFullscreen = false }
                    ) {
                        Image(
                            painter = qrPainter,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                        )

                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
                    .widthIn(150.dp, 300.dp)
                    .aspectRatio(1f)
                    .combinedClickable(
                        onClick = {
                            isFullscreen = true
                        },
                        onLongClick = {
                            isFullscreen = true
                        },
                    )
            ) {
                qrPainter?.also {
                    Image(
                        painter = it,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    )
                }
            }

            GreenButton(
                text = stringResource(id = R.string.id_next),
                modifier = Modifier.fillMaxWidth(),
                size = GreenButtonSize.BIG
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}

@Composable
@Preview
fun ExportLightningKeyScreenPreview() {
    GreenTheme {
        ExportLightningKeyScreen(ExportLightningKeyViewModelPreview.preview())
    }
}