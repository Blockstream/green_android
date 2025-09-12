package com.blockstream.compose.screens.exchange

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.get
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_buy_s
import blockstream_green.common.generated.resources.id_exchange
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowSquareOut
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.exchange.BuyViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenAccountSelector
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.MeldProvider
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.green20
import com.blockstream.compose.utils.OpenKeyboard
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.green.data.meld.data.QuoteResponse
import com.blockstream.green.data.meld.models.Country
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.LocalNavigator
import com.blockstream.ui.navigation.bottomsheet.BottomSheetNavigator
import com.blockstream.ui.navigation.getResult
import org.jetbrains.compose.resources.stringResource

@Composable
fun BuyScreen(
    viewModel: BuyViewModelAbstract
) {
    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    NavigateDestinations.MeldCountries.getResult<Country> {
        viewModel.changeCountry(it)
    }

    NavigateDestinations.Accounts.getResult<AccountAssetBalance> {
        viewModel.accountAsset.value = it.accountAsset
    }

    NavigateDestinations.BuyQuotes.getResult<QuoteResponse> {
        viewModel.changeQuote(it)
    }

    val focusRequester = remember { FocusRequester() }
    OpenKeyboard(focusRequester)

    val showAccountSelector by viewModel.showAccountSelector.collectAsStateWithLifecycle()
    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val onProgressQuote by viewModel.onProgressQuote.collectAsStateWithLifecycle()
    val onProgressBuy by viewModel.onProgressBuy.collectAsStateWithLifecycle()
    val quote by viewModel.quote.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val showRecoveryConfirmation by viewModel.showRecoveryConfirmation.collectAsStateWithLifecycle()

    val bottomSheetNavigator = LocalNavigator.current.navigatorProvider[BottomSheetNavigator::class]

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        withImePadding = true,
        onProgressStyle = if (onProgressBuy) OnProgressStyle.Full(bluBackground = false) else OnProgressStyle.Disabled,
        sideEffectsHandler = {
            when (it) {
                is SideEffects.Dismiss -> {
                    bottomSheetNavigator.popBackStack()
                }
            }
        }
    ) {

        if (showRecoveryConfirmation) {
            GreenAlert(
                alertType = AlertType.RecoveryIsUnconfirmed(withCloseButton = true),
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        GreenColumn {

            GreenColumn(
                space = 24,
                padding = 0,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            ) {

                val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()

                val amount by viewModel.amount.collectAsStateWithLifecycle()
                val amountHint by viewModel.amountHint.collectAsStateWithLifecycle()
                val denomination by viewModel.denomination.collectAsStateWithLifecycle()
                val suggestedAmounts by viewModel.suggestedAmounts.collectAsStateWithLifecycle()

                GreenColumn(padding = 0, space = 5) {
                    GreenAmountField(
                        value = amount,
                        onValueChange = viewModel.amount.onValueChange(),
                        secondaryValue = if (onProgressQuote) null else (quote?.destinationAmount?.let { "$it ${quote?.destinationCurrencyCode}".trim() }
                            ?: ""),
                        assetId = accountAsset?.assetId,
                        session = viewModel.sessionOrNull,
                        denomination = denomination,
                        focusRequester = focusRequester,
                        helperText = amountHint,
                        helperContainerColor = green20,
                    )

                    GreenRow(padding = 0, space = 8) {
                        suggestedAmounts.forEachIndexed { index, it ->
                            GreenButton(
                                text = it,
                                size = GreenButtonSize.NORMAL,
                                type = GreenButtonType.OUTLINE,
                                color = if (it == amount) GreenButtonColor.GREENER else GreenButtonColor.GREEN,
                                modifier = Modifier.weight(1f),
                                testTag = "suggested_amount_" + index
                            ) {
                                viewModel.amount.value = it
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = amount.isNotBlank()) {

                    MeldProvider(
                        title = stringResource(Res.string.id_exchange),
                        quote = quote,
                        error = error,
                        onProgress = onProgressQuote,
                        withEditIcon = true,
                        onClick = {
                            viewModel.changeQuote()
                        }
                    )
                }

                accountAsset.takeIf { showAccountSelector }?.also {
                    GreenAccountSelector(account = it.account, onClick = {
                        viewModel.changeAccount()
                    })
                }
            }

            val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()

            accountAsset?.also { accountAsset ->
                GreenColumn(padding = 0) {
                    GreenButton(
                        text = stringResource(
                            Res.string.id_buy_s,
                            accountAsset.asset.nameOrNull(viewModel.sessionOrNull) ?: accountAsset.assetId
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        size = GreenButtonSize.BIG,
                        icon = PhosphorIcons.Regular.ArrowSquareOut,
                        enabled = buttonEnabled,
                        onProgress = onProgress
                    ) {
                        viewModel.buy()
                    }
                }
            }
        }
    }
}