package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_address
import blockstream_green.common.generated.resources.id_amount_you_will_receive
import blockstream_green.common.generated.resources.id_description
import blockstream_green.common.generated.resources.id_expiration
import blockstream_green.common.generated.resources.id_lightning_invoice
import blockstream_green.common.generated.resources.id_qr_code
import blockstream_green.common.generated.resources.id_share
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.blockstream.data.data.MenuEntry
import com.blockstream.data.data.MenuEntryList
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.models.receive.ReceiveViewModel
import com.blockstream.compose.models.receive.ReceiveViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightningInvoiceBottomSheet(
    viewModel: ReceiveViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val platformManager = rememberPlatformManager()

    val receiveAddress by viewModel.receiveAddress.collectAsStateWithLifecycle()
    val receiveAddressUri by viewModel.receiveAddressUri.collectAsStateWithLifecycle()
    val invoiceAmountToReceive by viewModel.invoiceAmountToReceive.collectAsStateWithLifecycle()
    val invoiceAmountToReceiveFiat by viewModel.invoiceAmountToReceiveFiat.collectAsStateWithLifecycle()
    val invoiceDescription by viewModel.invoiceDescription.collectAsStateWithLifecycle()
    val invoiceExpiration by viewModel.invoiceExpiration.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    NavigateDestinations.Menu.getResult<Int> {
        if (it == 0) {
            scope.launch {
                viewModel.postEvent(ReceiveViewModel.LocalEvents.ShareAddress)
            }
        } else {
            scope.launch {
                runCatching {
                    val qrCode: Painter = QrCodePainter(
                        data = receiveAddressUri ?: "",
                    )
                    val data = qrCode.toByteArray(800, 800).let { bytes ->
                        platformManager.processQr(bytes, receiveAddressUri ?: "")
                    }
                    viewModel.postEvent(event = ReceiveViewModel.LocalEvents.ShareQR(data = data))
                }
            }
        }
    }

    GreenBottomSheet(
        title = stringResource(Res.string.id_lightning_invoice),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            GreenQR(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                onQrClick = {
                    viewModel.postEvent(ReceiveViewModel.LocalEvents.CopyAddress)
                },
                data = receiveAddressUri,
            )

            GreenColumn(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GreenAddress(
                    address = receiveAddress ?: "",
                    textAlign = TextAlign.Center,
                    showCopyIcon = true,
                    maxLines = 1,
                    onCopyClick = {
                        viewModel.postEvent(ReceiveViewModel.LocalEvents.CopyAddress)
                    }
                )
            }

            GreenColumn(padding = 0) {
                invoiceAmountToReceive?.also {
                    Column {
                        Text(
                            stringResource(Res.string.id_amount_you_will_receive),
                            style = labelMedium,
                            color = whiteMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                it,
                                style = titleSmall,
                                color = whiteHigh,
                                modifier = Modifier.weight(1f)
                            )
                            invoiceAmountToReceiveFiat?.also { fiat ->
                                Text(text = fiat, style = labelMedium, color = whiteLow)
                            }
                        }
                    }
                }

                invoiceDescription?.also {
                    Column {
                        Text(
                            stringResource(Res.string.id_description),
                            style = labelMedium,
                            color = whiteMedium
                        )
                        Text(
                            it,
                            style = bodyLarge,
                            color = whiteHigh,
                        )
                    }
                }

                invoiceExpiration?.also {
                    Column {
                        Text(
                            stringResource(Res.string.id_expiration),
                            style = labelMedium,
                            color = whiteMedium
                        )
                        Text(
                            it,
                            style = bodyLarge,
                            color = whiteHigh,
                        )
                    }
                }
            }

            GreenButton(
                text = stringResource(Res.string.id_share),
                icon = PhosphorIcons.Regular.ShareNetwork,
                modifier = Modifier.fillMaxWidth(),
                enabled = !onProgress
            ) {
                scope.launch {
                    viewModel.postEvent(
                        NavigateDestinations.Menu(
                            title = getString(Res.string.id_share),
                            entries = MenuEntryList(
                                listOf(
                                    MenuEntry(
                                        title = getString(Res.string.id_address),
                                        iconRes = "text-aa"
                                    ),
                                    MenuEntry(
                                        title = getString(Res.string.id_qr_code),
                                        iconRes = "qr-code"
                                    ),
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}
