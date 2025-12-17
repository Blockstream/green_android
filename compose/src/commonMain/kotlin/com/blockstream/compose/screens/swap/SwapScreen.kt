package com.blockstream.compose.screens.swap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_fee_rate
import blockstream_green.common.generated.resources.id_next
import blockstream_green.common.generated.resources.id_set_custom_fee_rate
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.NetworkFeeLine
import com.blockstream.compose.components.SwapComponent
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.models.swap.SwapViewModelAbstract
import com.blockstream.compose.models.swap.SwapViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.utils.OpenKeyboard
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.stringResourceFromIdOrNull
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.FeePriority
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.utils.DecimalFormat
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SwapScreen(
    viewModel: SwapViewModelAbstract
) {

    NavigateDestinations.Accounts.getResult<AccountAssetBalance> {
        viewModel.setAccount(it)
    }

    NavigateDestinations.FeeRate.getResult<FeePriority> {
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetFeeRate(it))
    }

    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    var customFeeDialog by remember { mutableStateOf<String?>(null) }

    val decimalSymbol = remember { DecimalFormat.DecimalSeparator }

    if (customFeeDialog != null) {
        TextDialog(
            title = stringResource(Res.string.id_set_custom_fee_rate),
            label = stringResource(Res.string.id_fee_rate),
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
    val focusRequester = remember { FocusRequester() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val denomination by viewModel.denomination.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    AnimatedVisibility(visible = onProgress) {
        LinearProgressIndicator(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
        )
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        withImePadding = true,
        sideEffectsHandler = {
            if (it is CreateTransactionViewModelAbstract.LocalSideEffects.ShowCustomFeeRate) {
                customFeeDialog = it.feeRate.toString()
            }
        }
    ) {

        GreenColumn(space = 8) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                AnimatedVisibility(uiState.from != null && uiState.to != null) {

                    OpenKeyboard(focusRequester)

                    SwapComponent(
                        from = uiState.from!!,
                        to = uiState.to!!,
                        fromAccounts = uiState.fromAccounts,
                        toAccounts = uiState.toAccounts,
                        amountFrom = uiState.amountFrom,
                        amountFromFiat = uiState.amountFromExchange,
                        amountTo = uiState.amountTo,
                        amountToFiat = uiState.amountToExchange,
                        onAmountChanged = viewModel::onAmountChanged,
                        denomination = denomination,
                        session = viewModel.session,
                        focusRequester = focusRequester,
                        error = stringResourceFromIdOrNull(uiState.error),
                        onFromAccountClick = {
                            viewModel.onAccountClick(isFrom = true)
                        },
                        onFromAssetClick = {

                        },
                        onToAccountClick = {
                            viewModel.onAccountClick(isFrom = false)
                        },
                        onToAssetClick = {

                        },
                        onTogglePairsClick = {
                            viewModel.swapPairs()
                        },
                        onDenominationClick = {
                            viewModel.postEvent(Events.SelectDenomination)
                        }
                    )
                }
            }

            val showFeeSelector by viewModel.showFeeSelector.collectAsStateWithLifecycle()
            val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()
            AnimatedVisibility(showFeeSelector) {
                NetworkFeeLine(
                    feePriority = feePriority, onClick = {
                        viewModel.postEvent(
                            CreateTransactionViewModelAbstract.LocalEvents.ClickFeePriority(isFeeRateOnly = true)
                        )
                    }
                )
            }

            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
            GreenButton(
                text = stringResource(Res.string.id_next),
                size = GreenButtonSize.BIG,
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.createSwap()
            }
        }
    }
}

@Composable
@Preview
fun SwapScreenPreview() {
    GreenPreview {
        SwapScreen(viewModel = SwapViewModelPreview.preview())
    }
}
