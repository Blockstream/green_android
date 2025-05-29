package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import blockstream_green.common.generated.resources.id_total_spent
import blockstream_green.common.generated.resources.id_verify_address_on_device
import blockstream_green.common.generated.resources.id_your_redeposit_address
import blockstream_green.common.generated.resources.pencil_simple_line
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.SendConfirmViewModel
import com.blockstream.common.models.send.SendConfirmViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenConfirmButton
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.screens.jade.JadeQRResult
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.LocalNavigator
import com.blockstream.ui.navigation.bottomsheet.BottomSheetNavigator
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SendConfirmScreen(
    viewModel: SendConfirmViewModelAbstract
) {
    val look by viewModel.transactionConfirmLook.collectAsStateWithLifecycle()
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
            padding = 0,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Banner(viewModel)


                look?.from?.also {
                    GreenAccountAsset(
                        accountAssetBalance = it.accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        title = stringResource(Res.string.id_from),
                        withAsset = false
                    )
                }

                look?.to?.also {
                    GreenAccountAsset(
                        accountAssetBalance = it.accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        title = stringResource(Res.string.id_to),
                        withAsset = false
                    )
                }

                look?.amount?.also {
                    GreenAmount(
                        title = stringResource(Res.string.id_amount),
                        amount = it,
                        amountFiat = look?.amountFiat,
                    )
                }

                look?.utxos?.forEach {
                    GreenAmount(
                        title = stringResource(if (look?.isRedeposit == true) Res.string.id_your_redeposit_address else Res.string.id_sent_to),
                        amount = it.amount ?: "",
                        amountFiat = it.amountExchange,
                        assetId = it.assetId,
                        address = it.address,
                        session = viewModel.sessionOrNull,
                        showIcon = true
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
                        title = stringResource(Res.string.id_note),
                        withPadding = false
                    ) {
                        Row {
                            Text(
                                text = note, modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp)
                                    .padding(start = 16.dp)
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

            Row {
                Column {
                    Text(
                        stringResource(Res.string.id_network_fee),
                        color = whiteMedium,
                        style = labelLarge
                    )
                    look?.feeRate?.also {
                        Text(
                            text = it,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = whiteMedium,
                            style = bodySmall
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    look?.fee?.also {
                        Text(it, color = whiteHigh, style = labelLarge)
                    }
                    look?.feeFiat?.also {
                        Text(it, color = whiteMedium, style = labelMedium)
                    }
                }
            }
            look?.total?.also { total ->
                HorizontalDivider()

                Row {
                    Text(
                        stringResource(Res.string.id_total_spent),
                        color = whiteHigh,
                        modifier = Modifier.weight(1f),
                        style = titleSmall
                    )
                    Column(horizontalAlignment = Alignment.End) {

                        Text(total, color = whiteHigh, style = titleSmall)

                        look?.totalFiat?.also {
                            Text(it, color = whiteMedium, style = labelLarge)
                        }
                    }
                }
            }


            GreenConfirmButton(viewModel = viewModel) {
                viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SignTransaction())
            }
        }
    }
}