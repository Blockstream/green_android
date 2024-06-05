package com.blockstream.compose.screens.lightning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.at
import blockstream_green.common.generated.resources.id_amount_to_be_refunded
import blockstream_green.common.generated.resources.id_fee_rate
import blockstream_green.common.generated.resources.id_receive_on
import blockstream_green.common.generated.resources.id_receive_on_address
import blockstream_green.common.generated.resources.id_refundable
import blockstream_green.common.generated.resources.id_set_custom_fee_rate
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.toggle
import com.blockstream.common.models.lightning.RecoverFundsViewModel
import com.blockstream.common.models.lightning.RecoverFundsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenIconButton
import com.blockstream.compose.components.GreenNetworkFee
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.components.ScreenContainer
import com.blockstream.compose.components.SlideToUnlock
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.AccountsBottomSheet
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


data class RecoverFundsScreen(
    val greenWallet: GreenWallet,
    val isSendAll: Boolean,
    val address: String?,
    val amount: Long
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<RecoverFundsViewModel>() {
            parametersOf(greenWallet, isSendAll, address, amount)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        RecoverFundsScreen(viewModel = viewModel)
    }
}

@Composable
fun RecoverFundsScreen(
    viewModel: RecoverFundsViewModelAbstract,
) {
    AccountsBottomSheet.getResult {
        viewModel.postEvent(Events.SetAccountAsset(it.accountAsset))
    }

    CameraBottomSheet.getResult {
        viewModel.manualAddress.value = it.result
    }

    FeeRateBottomSheet.getResult {
        viewModel.postEvent(RecoverFundsViewModel.LocalEvents.SetFeeRate(it))
    }

    var customFeeDialog by remember { mutableStateOf<String?>(null) }

    HandleSideEffect(viewModel) {
        if (it is RecoverFundsViewModel.LocalSideEffects.ShowCustomFeeRate) {
            customFeeDialog = it.feeRate.toString()
        }
    }

    customFeeDialog?.also {
        TextDialog(
            title = stringResource(Res.string.id_set_custom_fee_rate),
            label = stringResource(Res.string.id_fee_rate),
            placeholder = "0",
            initialText = it,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            supportingText = "Fee rate per vbyte"
        ) { value ->
            customFeeDialog = null

            if (value != null) {
                viewModel.postEvent(
                    RecoverFundsViewModel.LocalEvents.SetCustomFeeRate(value)
                )
            }
        }
    }

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()

    ScreenContainer(
        onProgress = onProgressSending,
        blurBackground = true,
        riveAnimation = RiveAnimation.LIGHTNING_TRANSACTION
    ) {
        AnimatedVisibility(
            visible = onProgress && !onProgressSending,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
        }

        Column {

            GreenColumn(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()
                val showManualAddress by viewModel.showManualAddress.collectAsStateWithLifecycle()
                val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current

                Column {
                    if (showManualAddress) {
                        val manualAddress by viewModel.manualAddress.collectAsStateWithLifecycle()
                        GreenTextField(
                            modifier = Modifier.padding(bottom = 8.dp),
                            title = stringResource(Res.string.id_receive_on),
                            value = manualAddress,
                            onValueChange = viewModel.manualAddress.onValueChange(),
                            singleLine = false,
                            maxLines = 3,
//                            error = errorAddress,
                            onQrClick = {
                                bottomSheetNavigator?.show(
                                    CameraBottomSheet(
                                        isDecodeContinuous = true,
                                        parentScreenName = viewModel.screenName(),
                                    )
                                )
                            }
                        )

                    } else {
                        GreenAccountAsset(
                            title = stringResource(Res.string.id_receive_on),
                            accountAssetBalance = accountAsset?.accountAssetBalance,
                            session = viewModel.sessionOrNull,
                            withAsset = false,
                            withEditIcon = true
                        ) {
                            viewModel.postEvent(
                                NavigateDestinations.Accounts(
                                    accounts = viewModel.bitcoinAccounts.value,
                                    withAsset = false
                                )
                            )
                        }
                    }

                    GreenIconButton(
                        modifier = Modifier.align(Alignment.End),
                        text = stringResource(Res.string.id_receive_on_address),
                        icon = painterResource(Res.drawable.at),
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = 0.dp,
                            start = 8.dp,
                            end = 8.dp
                        ),
                        color = if (showManualAddress) green else whiteMedium,
                    ) {
                        viewModel.showManualAddress.toggle()
                    }

                    val amount by viewModel.amount.collectAsStateWithLifecycle()
                    GreenAmount(
                        title = stringResource(Res.string.id_refundable),
                        amount = amount,
                        session = viewModel.sessionOrNull
                    )
                }

                val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()
                GreenNetworkFee(feePriority = feePriority, onClick = { onIconClicked ->
                    viewModel.postEvent(
                        RecoverFundsViewModel.LocalEvents.ClickFeePriority(
                            showCustomFeeRateDialog = onIconClicked
                        )
                    )
                }
                )

                val amountToBeRefunded by viewModel.amountToBeRefunded.collectAsStateWithLifecycle()
                val amountToBeRefundedFiat by viewModel.amountToBeRefundedFiat.collectAsStateWithLifecycle()

                AnimatedNullableVisibility(value = amountToBeRefunded) {
                    GreenAmount(
                        title = stringResource(Res.string.id_amount_to_be_refunded),
                        amount = it,
                        amountFiat = amountToBeRefundedFiat,
                        session = viewModel.sessionOrNull
                    )
                }

                val error by viewModel.error.collectAsStateWithLifecycle()

                AnimatedNullableVisibility(value = error) {
                    Text(text = it, color = red)
                }
            }

            GreenColumn {
                val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
                SlideToUnlock(
                    isLoading = onProgressSending,
                    enabled = buttonEnabled,
                    onSlideComplete = {
                         viewModel.postEvent(Events.Continue)
                    }
                )
            }
        }
    }
}