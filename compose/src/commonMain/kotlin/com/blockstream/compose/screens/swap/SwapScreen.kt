package com.blockstream.compose.screens.swap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_fee_rate
import blockstream_green.common.generated.resources.id_have_a_stuck_swap
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_new_swaps_are_temporarily_disabled
import blockstream_green.common.generated.resources.id_reset_stuck_swaps
import blockstream_green.common.generated.resources.id_set_custom_fee_rate
import blockstream_green.common.generated.resources.id_swaps_unavailable
import blockstream_green.common.generated.resources.swaps_unavailable
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
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
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.OpenKeyboard
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.appTestTag
import com.blockstream.compose.utils.stringResourceFromIdOrNull
import com.blockstream.data.Urls
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.FeePriority
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.AssetBalance
import com.blockstream.data.utils.DecimalFormat
import com.blockstream.data.utils.FEE_RATE_DECIMALS
import com.blockstream.data.utils.userNumberFormat
import com.blockstream.domain.swap.isSwapPairSupported
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SwapScreen(
    viewModel: SwapViewModelAbstract
) {
    NavigateDestinations.Accounts.getResult<AccountAssetBalance> {
        viewModel.setAccount(it)
    }

    NavigateDestinations.SwapAssets.getResult<AssetBalance> {
        viewModel.setAsset(it)
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
            initialText = viewModel.customFeeRate.value?.let {
                userNumberFormat(decimals = FEE_RATE_DECIMALS, withDecimalSeparator = false).format(it)
            } ?: "",
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            supportingText = "Fee rate per vbyte",
            maxFractionDigits = FEE_RATE_DECIMALS
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

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        withImePadding = uiState.isSwapCreationAvailable,
        sideEffectsHandler = {
            if (it is CreateTransactionViewModelAbstract.LocalSideEffects.ShowCustomFeeRate) {
                customFeeDialog = it.feeRate.toString()
            }
        }
    ) {
        if (uiState.isSwapCreationAvailable) {
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
                            fromBalance = uiState.fromBalance,
                            to = uiState.to!!,
                            toBalance = uiState.toBalance,
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
                            errorSide = uiState.errorSide,
                            isPairSupported = isSwapPairSupported(uiState.from!!, uiState.to!!) &&
                                    viewModel.isDirectionAvailable(uiState.from!!, uiState.to!!),
                            onFromAccountClick = {
                                viewModel.onAccountClick(isFrom = true)
                            },
                            onFromAssetClick = {
                                viewModel.onAssetClick(isFrom = true)
                            },
                            onToAccountClick = {
                                viewModel.onAccountClick(isFrom = false)
                            },
                            onToAssetClick = {
                                viewModel.onAssetClick(isFrom = false)
                            },
                            onTogglePairsClick = {
                                viewModel.swapPairs()
                            },
                            onDenominationClick = { isSendQuoteMode ->
                                viewModel.onQuoteModeChanged(isSendQuoteMode = isSendQuoteMode)
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
                    text = stringResource(Res.string.id_continue),
                    size = GreenButtonSize.BIG,
                    enabled = buttonEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.createSwap()
                }
            }
        } else {
            val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

            SwapsUnavailable(
                hasSwaps = uiState.hasSwaps,
                onProgress = onProgress,
                onResetClick = {
                    viewModel.resetSwaps()
                },
                onLearnMoreClick = {
                    viewModel.postEvent(Events.OpenBrowser(Urls.HELP_SWAPS_UNAVAILABLE))
                }
            )
        }
    }
}

@Composable
private fun SwapsUnavailable(
    hasSwaps: Boolean,
    onProgress: Boolean,
    onResetClick: () -> Unit,
    onLearnMoreClick: () -> Unit
) {
    GreenColumn(space = 8, modifier = Modifier.appTestTag("swaps_unavailable")) {
        GreenColumn(
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            padding = 0,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier.size(128.dp),
                painter = painterResource(Res.drawable.swaps_unavailable),
                contentDescription = null
            )

            GreenColumn(padding = 0, space = 6, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(Res.string.id_swaps_unavailable), style = titleMedium)
                Text(
                    text = stringResource(Res.string.id_new_swaps_are_temporarily_disabled),
                    color = textMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (hasSwaps) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.id_have_a_stuck_swap),
                textAlign = TextAlign.Center,
                color = whiteLow
            )

            GreenButton(
                text = stringResource(Res.string.id_reset_stuck_swaps),
                modifier = Modifier.fillMaxWidth(),
                size = GreenButtonSize.BIG,
                enabled = !onProgress,
                onProgress = onProgress,
                testTag = "reset_stuck_swaps",
                onClick = onResetClick
            )
        }

        GreenButton(
            text = stringResource(Res.string.id_learn_more),
            modifier = Modifier.fillMaxWidth(),
            size = GreenButtonSize.BIG,
            type = GreenButtonType.TEXT,
            onClick = onLearnMoreClick
        )
    }
}

@Composable
@Preview
fun SwapScreenPreview() {
    GreenPreview {
        SwapScreen(viewModel = SwapViewModelPreview.preview())
    }
}

@Composable
@Preview
fun SwapScreenUnavailablePreview() {
    GreenPreview {
        SwapScreen(viewModel = SwapViewModelPreview.previewUnavailable(hasSwaps = true))
    }
}

@Composable
@Preview
fun SwapScreenUnavailableNoSwapsPreview() {
    GreenPreview {
        SwapScreen(viewModel = SwapViewModelPreview.previewUnavailable(hasSwaps = false))
    }
}
