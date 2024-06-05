package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_fee_rate
import blockstream_green.common.generated.resources.id_private_key
import blockstream_green.common.generated.resources.id_receive_in
import blockstream_green.common.generated.resources.id_set_custom_fee_rate
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.AddressInputType
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.SweepViewModel
import com.blockstream.common.models.send.SweepViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.common.utils.stringResourceFromId
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenNetworkFee
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.components.SlideToUnlock
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.sheets.AccountsBottomSheet
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.md_theme_onError
import com.blockstream.compose.theme.md_theme_onErrorContainer
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class SweepScreen(
    val greenWallet: GreenWallet,
    val privateKey: String? = null,
    val accountAsset: AccountAsset? = null
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SweepViewModel> {
            parametersOf(greenWallet, privateKey, accountAsset)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        SweepScreen(viewModel = viewModel)
    }
}

@Composable
fun SweepScreen(
    viewModel: SweepViewModelAbstract
) {
    CameraBottomSheet.getResult {
        viewModel.privateKey.value = it.result
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetAddressInputType(AddressInputType.SCAN))
    }

    AccountsBottomSheet.getResult {
        viewModel.postEvent(Events.SetAccountAsset(it.accountAsset))
    }

    FeeRateBottomSheet.getResult {
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetFeeRate(it))
    }

    var customFeeDialog by remember { mutableStateOf<String?>(null) }

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    HandleSideEffect(viewModel) {
        if (it is CreateTransactionViewModelAbstract.LocalSideEffects.ShowCustomFeeRate) {
            customFeeDialog = it.feeRate.toString()
        }
    }

    val decimalSymbol = remember { DecimalFormat.DecimalSeparator }

    if (customFeeDialog != null) {
        TextDialog(
            title = stringResource(Res.string.id_set_custom_fee_rate),
            label = stringResource(Res.string.id_fee_rate),
            placeholder = "0${decimalSymbol}00" ,
            initialText = viewModel.customFeeRate.value?.toString() ?: "",
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            supportingText = "Fee rate per vbyte"
        ) { value ->
            customFeeDialog = null

            if (value != null) {
                viewModel.postEvent(
                    CreateTransactionViewModelAbstract.LocalEvents.SetCustomFeeRate(
                        value
                    )
                )
            }
        }
    }

    val error by viewModel.error.collectAsStateWithLifecycle()

    GreenColumn {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            val privateKey by viewModel.privateKey.collectAsStateWithLifecycle()
            GreenTextField(
                title = stringResource(Res.string.id_private_key),
                value = privateKey,
                onValueChange = {
                    viewModel.privateKey.value = it
                    viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetAddressInputType(AddressInputType.SCAN))
                },
                singleLine = false,
                error = error.takeIf { listOf("id_invalid_private_key").contains(it) }?.let { stringResourceFromId(it) },
                onQrClick = {
                    bottomSheetNavigator?.show(
                        CameraBottomSheet(
                            isDecodeContinuous = true,
                            parentScreenName = viewModel.screenName(),
                        )
                    )
                }
            )

            val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()

            GreenAccountAsset(
                title = stringResource(Res.string.id_receive_in),
                accountAssetBalance = accountAsset?.accountAssetBalance,
                session = viewModel.sessionOrNull,
                withAsset = false,
                withEditIcon = true
            ) {
                viewModel.postEvent(
                    NavigateDestinations.Accounts(
                        accounts = viewModel.accounts.value,
                        withAsset = false
                    )
                )
            }


            val amount by viewModel.amount.collectAsStateWithLifecycle()
            val amountFiat by viewModel.amountFiat.collectAsStateWithLifecycle()

            AnimatedNullableVisibility(value = amount) {
                GreenAmount(
                    title = stringResource(Res.string.id_amount),
                    amount = it,
                    amountFiat = amountFiat
                )
            }

            val showFeeSelector by viewModel.showFeeSelector.collectAsStateWithLifecycle()
            val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()
            AnimatedVisibility(visible = showFeeSelector) {
                GreenNetworkFee(
                    feePriority = feePriority, onClick = { onIconClicked ->
                        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.ClickFeePriority(showCustomFeeRateDialog = onIconClicked))
                    }
                )
            }

            AnimatedNullableVisibility(value = error.takeIf { !listOf("id_invalid_private_key", "id_insufficient_funds").contains(it) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = md_theme_onErrorContainer,
                        contentColor = md_theme_onError
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                    ) {
                        Text(text = stringResourceFromId(it))
                    }
                }
            }

        }


        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        SlideToUnlock(isLoading = onProgress, enabled = buttonEnabled, onSlideComplete = {
            viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SignTransaction())
        })
    }
}