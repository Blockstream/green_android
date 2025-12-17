package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.get
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_from
import blockstream_green.common.generated.resources.id_network_fee
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_sent_to
import blockstream_green.common.generated.resources.id_to
import blockstream_green.common.generated.resources.id_total_fees
import blockstream_green.common.generated.resources.id_total_spent
import blockstream_green.common.generated.resources.id_total_to_receive
import blockstream_green.common.generated.resources.id_verify_address_on_device
import blockstream_green.common.generated.resources.id_you_are_paying_this_lightning_invoice
import blockstream_green.common.generated.resources.id_your_redeposit_address
import blockstream_green.common.generated.resources.info
import blockstream_green.common.generated.resources.pencil_simple_line
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenConfirmButton
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.models.send.SendConfirmViewModel
import com.blockstream.compose.models.send.SendConfirmViewModelAbstract
import com.blockstream.compose.models.send.SendConfirmViewModelPreview
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.bottomsheet.BottomSheetNavigator
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.screens.jade.JadeQRResult
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.transaction.TransactionConfirmation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendConfirmScreen(
    viewModel: SendConfirmViewModelAbstract
) {
    val look by viewModel.transactionConfirmation.collectAsStateWithLifecycle()
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()
    val bottomSheetNavigator = LocalNavigator.current.navigatorProvider[BottomSheetNavigator::class]

    NavigateDestinations.Note.getResult<String> {
        viewModel.note.value = it
    }

    NavigateDestinations.JadeQR.getResult<JadeQRResult> {
        viewModel.postEvent(
            CreateTransactionViewModelAbstract.LocalEvents.BroadcastTransaction(
                psbt = it.result
            )
        )
    }

    NavigateDestinations.Login.getResult<GreenWallet> {
        viewModel.executePendingAction()
    }

    SetupScreen(
        viewModel = viewModel,
        onProgressStyle = if (onProgressSending) OnProgressStyle.Full(bluBackground = true) else OnProgressStyle.Disabled,
        withPadding = false,
        sideEffectsHandler = {
            when (it) {
                is SideEffects.Dismiss -> {
                    bottomSheetNavigator.popBackStack()
                }
            }
        }) {
        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

        GreenColumn(
            padding = 0, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp)
        ) {
            look?.also { look ->
                GreenColumn(
                    padding = 0, modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                ) {
                    Banner(viewModel)

                    look.from?.also {
                        GreenAccountAsset(
                            accountAssetBalance = it.accountAssetBalance,
                            session = viewModel.sessionOrNull,
                            title = stringResource(Res.string.id_from),
                            withAsset = false
                        )
                    }

                    look.to?.also {
                        GreenAccountAsset(
                            accountAssetBalance = it.accountAssetBalance,
                            session = viewModel.sessionOrNull,
                            title = stringResource(Res.string.id_to),
                            withAsset = false
                        )
                    }

                    look.amount?.also {
                        GreenAmount(
                            title = stringResource(Res.string.id_amount),
                            amount = it,
                            amountFiat = look.amountFiat,
                        )
                    }

                    look.utxos?.forEach {
                        GreenAmount(
                            title = stringResource(if (look?.isRedeposit == true) Res.string.id_your_redeposit_address else Res.string.id_sent_to),
                            amount = it.amount ?: "",
                            amountFiat = it.amountExchange,
                            assetId = it.assetId,
                            address = it.address,
                            addressMaxLines = if (look?.isLiquidToLightningSwap == true) 1 else null,
                            session = viewModel.sessionOrNull,
                            showIcon = true
                        )
                    }

                    if (look.isLiquidToLightningSwap) {
                        Text(
                            text = stringResource(Res.string.id_you_are_paying_this_lightning_invoice),
                            color = whiteMedium,
                            style = bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    val showVerifyOnDevice by viewModel.showVerifyOnDevice.collectAsStateWithLifecycle()
                    if (showVerifyOnDevice) {
                        GreenButton(
                            text = stringResource(Res.string.id_verify_address_on_device),
                            enabled = buttonEnabled,
                            type = GreenButtonType.OUTLINE,
                            color = GreenButtonColor.WHITE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.postEvent(SendConfirmViewModel.LocalEvents.VerifyOnDevice)
                        }
                    }

                    val note by viewModel.note.collectAsStateWithLifecycle()
                    AnimatedVisibility(visible = note.isNotBlank()) {
                        GreenDataLayout(
                            title = stringResource(Res.string.id_note), withPadding = false
                        ) {
                            Row {
                                Text(
                                    text = note, modifier = Modifier.weight(1f).padding(vertical = 16.dp).padding(start = 16.dp)
                                )
                                IconButton(onClick = {
                                    viewModel.postEvent(SendConfirmViewModel.LocalEvents.Note)
                                }) {
                                    Icon(
                                        painter = painterResource(Res.drawable.pencil_simple_line),
                                        contentDescription = "Edit",
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    )
                                }
                            }
                        }
                    }
                }

                FeesAndTotalSection(look) {
                    viewModel.postEvent(SendConfirmViewModel.LocalEvents.ClickTotalFees)
                }
            }

            GreenConfirmButton(viewModel = viewModel) {
                viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SignTransaction())
            }
        }
    }
}

@Composable
private fun DataRow(
    title: String,
    subtitle: String? = null,
    value: String,
    valueSecondary: String? = null,
    isLarge: Boolean = false,
    onTitleClick: (() -> Unit)? = null
) {
    Row {
        Column {
            Row {
                Text(
                    text = title, color = whiteMedium, style = if (isLarge) titleSmall else labelLarge
                )

                if (onTitleClick != null) {
                    IconButton(
                        onClick = {
                            onTitleClick()
                        }, modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.info),
                            contentDescription = null,
                            tint = whiteMedium,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle, modifier = Modifier.align(Alignment.CenterHorizontally), color = whiteMedium, style = bodySmall
                )
            }
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {

            Text(text = value, color = whiteHigh, style = if (isLarge) titleSmall else labelLarge)

            if (valueSecondary != null) {
                Text(text = valueSecondary, color = whiteMedium, style = if (isLarge) labelLarge else labelMedium)
            }
        }
    }
}

@Composable
private fun FeesAndTotalSection(look: TransactionConfirmation, onTotalFeesClick: () -> Unit) {

    if (look.isLiquidToLightningSwap || look.isSwap) {
        DataRow(title = stringResource(Res.string.id_total_fees), onTitleClick = onTotalFeesClick, value = look.totalFees ?: "")

        if (look.isSwap) {
            DataRow(
                title = stringResource(Res.string.id_total_spent), value = look.total ?: ""
            )
        }
        
    } else {
        DataRow(
            title = stringResource(Res.string.id_network_fee),
            subtitle = look.feeRate,
            value = look.fee ?: "",
            valueSecondary = look.feeFiat
        )
    }

    HorizontalDivider()

    if (look.isSwap) {
        DataRow(
            title = stringResource(Res.string.id_total_to_receive),
            value = look.recipientReceives ?: "",
            valueSecondary = look.recipientReceivesFiat,
            isLarge = true
        )
    } else {
        look.total?.also { total ->
            DataRow(
                title = stringResource(Res.string.id_total_spent), value = total, valueSecondary = look.totalFiat, isLarge = true
            )
        }
    }

}

@Composable
@Preview
fun SendConfirmScreenPreview() {
    GreenPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.preview())
    }
}

@Composable
@Preview
fun SendConfirmScreenLBTCSwapPreview() {
    GreenPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.previewLBTCSwap())
    }
}

@Composable
@Preview
fun SendConfirmScreenSwapPreview() {
    GreenPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.previewSwap())
    }
}

@Composable
@Preview
fun SendConfirmScreenExchangePreview() {
    GreenPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.previewAccountExchange())
    }
}
