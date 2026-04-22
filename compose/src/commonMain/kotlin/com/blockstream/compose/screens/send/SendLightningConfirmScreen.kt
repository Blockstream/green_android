package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.bitcoin_lightning
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_asset
import blockstream_green.common.generated.resources.id_lightning_bitcoin
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_recipient
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenConfirmButton
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.models.send.SendLightningConfirmViewModelAbstract
import com.blockstream.compose.models.send.SendLightningConfirmViewModelPreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.displaySmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.dialogs.TransactionFailedDialog
import com.blockstream.compose.dialogs.TransactionSuccessDialog
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendLightningConfirmScreen(
    viewModel: SendLightningConfirmViewModelAbstract,
) {
    val transactionConfirmation by viewModel.transactionConfirmation.collectAsStateWithLifecycle()
    val invoiceAmount by viewModel.invoiceAmount.collectAsStateWithLifecycle()
    val invoiceAmountFiat by viewModel.invoiceAmountFiat.collectAsStateWithLifecycle()
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()
    val successAmount by viewModel.successAmount.collectAsStateWithLifecycle()
    val failureMessage by viewModel.failureMessage.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()

    successAmount?.also {
        TransactionSuccessDialog(
            amount = it,
            onDismissRequest = viewModel::onSuccessAcknowledged,
        )
    }

    failureMessage?.also {
        TransactionFailedDialog(
            message = it,
            onDismissRequest = viewModel::onFailureAcknowledged,
        )
    }

    SetupScreen(
        viewModel = viewModel,
        onProgressStyle = if (onProgressSending) OnProgressStyle.Full(bluBackground = true) else OnProgressStyle.Disabled,
        withPadding = false,
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                GreenDataLayout(title = stringResource(Res.string.id_asset), withPadding = false) {
                    LightningAssetRowContent()
                }

                GreenDataLayout(title = stringResource(Res.string.id_recipient), withPadding = false) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 70.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        ChunkedInvoice(invoice = viewModel.invoice, isLightning = true)
                    }
                }

                (invoiceAmount ?: transactionConfirmation?.amount)?.also { amount ->
                    GreenDataLayout(title = stringResource(Res.string.id_amount), withPadding = false) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 70.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = amount,
                                style = displaySmall.copy(fontWeight = FontWeight.Medium),
                                color = whiteHigh,
                                textAlign = TextAlign.Center,
                            )
                            (invoiceAmountFiat ?: transactionConfirmation?.amountFiat)?.also {
                                Text(
                                    text = "≈ $it",
                                    style = bodyMedium,
                                    color = whiteMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = note.isNotBlank()) {
                    GreenDataLayout(
                        title = stringResource(Res.string.id_note),
                        withPadding = false,
                    ) {
                        Text(
                            text = note,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                    }
                }
            }

            GreenConfirmButton(viewModel = viewModel) {
                viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SignTransaction())
            }
        }
    }
}

@Composable
private fun LightningAssetRowContent() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.bitcoin_lightning),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(Res.string.id_lightning_bitcoin),
            style = bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = whiteHigh,
        )
    }
}

@Composable
@Preview
fun SendLightningConfirmScreenPreview() {
    GreenPreview {
        SendLightningConfirmScreen(viewModel = SendLightningConfirmViewModelPreview.preview())
    }
}
