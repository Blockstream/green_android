package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.blockstream_devices
import blockstream_green.common.generated.resources.id_blockstream_app_needs_the_master_blinding
import blockstream_green.common.generated.resources.id_change
import blockstream_green.common.generated.resources.id_confirm_on_your_device
import blockstream_green.common.generated.resources.id_fee
import blockstream_green.common.generated.resources.id_sent_to
import blockstream_green.common.generated.resources.id_this_is_a_temporary_swap_address
import blockstream_green.common.generated.resources.id_this_is_not_the_recipient_address
import blockstream_green.common.generated.resources.id_to_show_balances_and
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.actionIcon
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.models.SimpleGreenViewModel
import com.blockstream.compose.models.SimpleGreenViewModelPreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.textLow
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.Urls
import com.blockstream.data.gdk.data.UtxoView
import com.blockstream.data.transaction.TransactionConfirmation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInteractionBottomSheet(
    viewModel: SimpleGreenViewModel,
    transactionConfirmation: TransactionConfirmation? = null,
    verifyAddress: String? = null,
    isMasterBlindingKeyRequest: Boolean = false,
    message: StringHolder? = null,
    onDismissRequest: () -> Unit,
) {

    val title = when {
        isMasterBlindingKeyRequest -> null
        transactionConfirmation != null || verifyAddress != null -> stringResource(Res.string.id_confirm_on_your_device)
        else -> {
            message?.stringOrNull()
        }
    }

    val deviceIcon = viewModel.deviceOrNull?.let {
        if (transactionConfirmation != null || verifyAddress != null) {
            it.actionIcon()
        } else {
            it.icon()
        }
    }

    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)

    GreenBottomSheet(
        title = title,
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Image(
                painter = painterResource(deviceIcon ?: Res.drawable.blockstream_devices),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(160.dp)
                    .padding(bottom = 16.dp)
            )

            GreenColumn(
                padding = 0,
                space = 16,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                if (transactionConfirmation != null) {
                    transactionConfirmation.utxos?.forEach {
                        GreenAmount(
                            title = stringResource(if (it.isChange) Res.string.id_change else Res.string.id_sent_to),
                            amount = it.amount ?: "",
                            assetId = it.assetId,
                            address = it.address,
                            session = viewModel.sessionOrNull,
                            showIcon = true
                        )
                    }

                    transactionConfirmation.fee?.also {
                        GreenAmount(
                            title = stringResource(Res.string.id_fee),
                            amount = it,
                            assetId = transactionConfirmation.feeAssetId,
                            session = viewModel.sessionOrNull,
                            showIcon = true
                        )
                    }

                    if (transactionConfirmation.isSwap || transactionConfirmation.isLiquidToLightningSwap) {

                        GreenColumn(padding = 0, space = 8, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.id_this_is_not_the_recipient_address),
                                color = textMedium,
                                style = bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                stringResource(Res.string.id_this_is_a_temporary_swap_address),
                                color = textLow,
                                style = bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (verifyAddress != null) {
                    GreenAddress(address = verifyAddress)
                }

                if (isMasterBlindingKeyRequest) {

                    Column {

                        Text(
                            text = stringResource(Res.string.id_blockstream_app_needs_the_master_blinding),
                            color = whiteMedium,
                            textAlign = TextAlign.Center,
                            style = titleSmall,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(Res.string.id_to_show_balances_and),
                            color = whiteMedium,
                            textAlign = TextAlign.Center,
                            style = bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    LearnMoreButton {
                        viewModel.postEvent(Events.OpenBrowser(Urls.HELP_MASTER_BLINDING_KEY))
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun VerifyOnDeviceTransactionBottomSheetPreview() {
    GreenPreview {
        DeviceInteractionBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            verifyAddress = null,
            transactionConfirmation = TransactionConfirmation(
                isSwap = true,
                utxos = listOf(
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        isChange = true,
                        amount = "1 BTC"
                    ),
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        isChange = false,
                        amount = "2 BTC"
                    )
                ),
                fee = "1 BTC"
            ),
            onDismissRequest = { }
        )
    }
}

@Composable
@Preview
fun VerifyOnDeviceAddressBottomSheetPreview() {
    GreenPreview {
        DeviceInteractionBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            verifyAddress = "bc1tinyaddresstestonly",
            onDismissRequest = { }
        )
    }
}

@Composable
@Preview
fun VerifyOnDeviceMessageBottomSheetPreview() {
    GreenPreview {
        DeviceInteractionBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            message = StringHolder.create("id_check_your_device"),
            onDismissRequest = { }
        )
    }
}

@Composable
@Preview
fun VerifyOnDeviceMasterBlindingKeyBottomSheetPreview() {
    GreenPreview {
        DeviceInteractionBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            isMasterBlindingKeyRequest = true,
            onDismissRequest = { }
        )
    }
}