package com.blockstream.compose.screens.lightning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LnUrlWithdrawRequestSerializable
import com.blockstream.common.events.Events
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModelAbstract
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModelPreview
import com.blockstream.common.models.send.AccountExchangeViewModel
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


data class LnUrlWithdrawScreen(
    val greenWallet: GreenWallet,
    val requestData: LnUrlWithdrawRequestSerializable
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<LnUrlWithdrawViewModel>() {
            parametersOf(greenWallet, requestData.deserialize())
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        LnUrlWithdrawScreen(viewModel = viewModel)
    }
}

@Composable
fun LnUrlWithdrawScreen(
    viewModel: LnUrlWithdrawViewModelAbstract,
) {

    getNavigationResult<DenominatedValue>(DenominationBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    HandleSideEffect(viewModel)

    Box {

        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        AnimatedVisibility(
            visible = onProgress, modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        GreenColumn(space = 24) {
            Text(
                text = stringResourceId(viewModel.redeemMessage),
                textAlign = TextAlign.Center,
                style = labelLarge,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
            )

            val amount by viewModel.amount.collectAsStateWithLifecycle()
            val exchange by viewModel.exchange.collectAsStateWithLifecycle()
            val withdrawaLimits by viewModel.withdrawaLimits.collectAsStateWithLifecycle()
            val denomination by viewModel.denomination.collectAsStateWithLifecycle()
            val error by viewModel.error.collectAsStateWithLifecycle()

            GreenAmountField(
                value = amount,
                onValueChange = viewModel.amount.onValueChange(),
                title = stringResource(id = R.string.id_amount_to_receive),
                session = viewModel.sessionOrNull,
                denomination = denomination,
                enabled = !onProgress,
                isAmountLocked = viewModel.isAmountLocked,
                error = error,
                onSendAllClick = {
                    viewModel.postEvent(AccountExchangeViewModel.LocalEvents.ToggleIsSendAll)
                },
                footerContent = {
                    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = stringResourceId(withdrawaLimits),
                            textAlign = TextAlign.Start,
                            style = bodyMedium,
                            color = whiteLow,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = stringResourceId(exchange),
                            textAlign = TextAlign.End,
                            style = bodyMedium,
                            color = whiteLow
                        )
                    }
                },
                onDenominationClick = {
                    viewModel.postEvent(Events.SelectDenomination)
                }
            )

            val description by viewModel.description.collectAsStateWithLifecycle()
            GreenTextField(
                title = stringResource(R.string.id_description),
                value = description,
                onValueChange = viewModel.description.onValueChange(),
                enabled = !onProgress,
            )

            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
            GreenButton(
                text = stringResource(R.string.id_redeem),
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}

@Composable
@Preview
fun LnUrlWithdrawScreenPreview() {
    GreenPreview {
        LnUrlWithdrawScreen(viewModel = LnUrlWithdrawViewModelPreview.preview())
    }
}
