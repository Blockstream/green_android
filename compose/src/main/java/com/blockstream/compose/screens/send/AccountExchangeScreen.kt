package com.blockstream.compose.screens.send

import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.send.AccountExchangeViewModel
import com.blockstream.common.models.send.AccountExchangeViewModelAbstract
import com.blockstream.common.models.send.AccountExchangeViewModelPreview
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.GreenNetworkFee
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.AccountsBottomSheet
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.MainMenuBottomSheet
import com.blockstream.compose.sheets.MainMenuEntry
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_onError
import com.blockstream.compose.theme.md_theme_onErrorContainer
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Parcelize
data class AccountExchangeScreen(
    val greenWallet: GreenWallet,
    val accountAsset: AccountAsset? = null,
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<AccountExchangeViewModel> {
            parametersOf(greenWallet, accountAsset)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        AccountExchangeScreen(viewModel = viewModel)
    }
}

@Composable
fun AccountExchangeScreen(
    viewModel: AccountExchangeViewModelAbstract
) {

    getNavigationResult<AccountAssetBalance>(AccountsBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(AccountExchangeViewModel.LocalEvents.SetToAccount(it.accountAsset))
    }

    getNavigationResult<FeePriority>(FeeRateBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetFeeRate(it))
    }

    getNavigationResult<DenominatedValue>(DenominationBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    getNavigationResult<MainMenuEntry>(MainMenuBottomSheet.resultKey).value?.also {
        when (it) {
            MainMenuEntry.SCAN -> {
                viewModel.postEvent(
                    NavigateDestinations.Camera(
                        isDecodeContinuous = true,
                        parentScreenName = viewModel.screenName()
                    )
                )
            }

            MainMenuEntry.ACCOUNT_TRANSFER -> {
                viewModel.postEvent(NavigateDestinations.AccountExchange)
            }
        }
    }

    var customFeeDialog by remember { mutableStateOf<String?>(null) }

    HandleSideEffect(viewModel) {
        if (it is CreateTransactionViewModelAbstract.LocalSideEffects.ShowCustomFeeRate) {
            customFeeDialog = it.feeRate.toString()
        }
    }

    val decimalSymbol = remember { DecimalFormat.DecimalSeparator }

    if (customFeeDialog != null) {
        TextDialog(
            title = stringResource(R.string.id_set_custom_fee_rate),
            label = stringResource(id = R.string.id_fee_rate),
            placeholder = "0${decimalSymbol}00",
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

    val fromAccountAssetBalance by viewModel.fromAccountAssetBalance.collectAsStateWithLifecycle()
    val toAccountAsset by viewModel.toAccountAsset.collectAsStateWithLifecycle()
    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val amountExchange by viewModel.amountExchange.collectAsStateWithLifecycle()
    val denomination by viewModel.denomination.collectAsStateWithLifecycle()
    val errorAmount by viewModel.errorAmount.collectAsStateWithLifecycle()
    val isSendAll by viewModel.isSendAll.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    AnimatedVisibility(visible = onProgress) {
        LinearProgressIndicator(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
        )
    }

    GreenColumn {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            GreenColumn(padding = 0, space = 8) {

                GreenAccountAsset(
                    accountAssetBalance = fromAccountAssetBalance,
                    session = viewModel.sessionOrNull,
                    title = stringResource(id = R.string.id_from),
                    withEditIcon = true,
                    onClick = {
                        viewModel.postEvent(AccountExchangeViewModel.LocalEvents.ClickAccount(isFrom = true))
                    }
                )

                AnimatedVisibility(visible = fromAccountAssetBalance != null) {
                    GreenAmountField(
                        value = amount,
                        onValueChange = {
                            viewModel.isSendAll.value = false
                            viewModel.amount.value = it
                        },
                        title = stringResource(id = R.string.id_send),
                        assetId = fromAccountAssetBalance?.assetId,
                        session = viewModel.sessionOrNull,
                        error = errorAmount,
                        denomination = denomination,
                        sendAll = isSendAll,
                        onSendAllClick = {
                            viewModel.postEvent(AccountExchangeViewModel.LocalEvents.ToggleIsSendAll)
                        },
                        footerContent = {
                            Text(
                                text = amountExchange,
                                textAlign = TextAlign.End,
                                style = bodyMedium,
                                color = whiteLow,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        onDenominationClick = {
                            viewModel.postEvent(Events.SelectDenomination)
                        }
                    )
                }
            }

            AnimatedNullableVisibility(
                value = fromAccountAssetBalance,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_down),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colorFilter = ColorFilter.tint(green),
                    contentDescription = null
                )
            }

            GreenColumn(padding = 0, space = 8) {
                AnimatedNullableVisibility(value = fromAccountAssetBalance) {

                    GreenAccountAsset(
                        accountAssetBalance = toAccountAsset?.accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        title = stringResource(id = R.string.id_to),
                        withAsset = false,
                        withEditIcon = true,
                        onClick = {
                            viewModel.postEvent(
                                AccountExchangeViewModel.LocalEvents.ClickAccount(
                                    isFrom = false
                                )
                            )
                        }
                    )
                }

                val receiveAmount by viewModel.receiveAmount.collectAsStateWithLifecycle()
                val receiveAmountFiat by viewModel.receiveAmountExchange.collectAsStateWithLifecycle()
                AnimatedNullableVisibility(value = receiveAmount) {
                    GreenDataLayout(title = stringResource(R.string.id_receive)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()

                        ) {

                            SelectionContainer {
                                Text(text = it, style = titleLarge)
                            }

                            receiveAmountFiat?.also { fiat ->
                                SelectionContainer {
                                    Text(text = fiat, style = bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        val errorGeneric by viewModel.errorGeneric.collectAsStateWithLifecycle()
        AnimatedNullableVisibility(value = errorGeneric) {
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
                    Text(text = stringResourceId(it))
                }
            }
        }

        val showFeeSelector by viewModel.showFeeSelector.collectAsStateWithLifecycle()
        val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()
        AnimatedVisibility(
            visible = showFeeSelector,
            modifier = Modifier.offset(y = (-8).dp)
        ) {
            GreenNetworkFee(
                feePriority = feePriority, onClick = { onIconClicked ->
                    viewModel.postEvent(
                        CreateTransactionViewModelAbstract.LocalEvents.ClickFeePriority(
                            showCustomFeeRateDialog = onIconClicked
                        )
                    )
                }
            )
        }

        AnimatedNullableVisibility(value = fromAccountAssetBalance) {
            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

            GreenButton(
                text = stringResource(id = R.string.id_next),
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}

@Composable
@Preview
fun ExchangeScreenPreview() {
    GreenPreview {
        AccountExchangeScreen(viewModel = AccountExchangeViewModelPreview.preview())
    }
}
