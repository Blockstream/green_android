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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount_to_receive
import blockstream_green.common.generated.resources.id_description
import blockstream_green.common.generated.resources.id_redeem
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LnUrlWithdrawRequestSerializable
import com.blockstream.common.events.Events
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModel
import com.blockstream.common.models.lightning.LnUrlWithdrawViewModelAbstract
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


data class LnUrlWithdrawScreen(
    val greenWallet: GreenWallet,
    val requestData: LnUrlWithdrawRequestSerializable
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<LnUrlWithdrawViewModel>() {
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

    DenominationBottomSheet.getResult {
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
                text = viewModel.redeemMessage,
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
                title = stringResource(Res.string.id_amount_to_receive),
                session = viewModel.sessionOrNull,
                denomination = denomination,
                enabled = !onProgress,
                isAmountLocked = viewModel.isAmountLocked,
                helperText = error,
                footerContent = {
                    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = withdrawaLimits,
                            textAlign = TextAlign.Start,
                            style = bodyMedium,
                            color = whiteLow,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = exchange,
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
                title = stringResource(Res.string.id_description),
                value = description,
                onValueChange = viewModel.description.onValueChange(),
                enabled = !onProgress,
            )

            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
            GreenButton(
                text = stringResource(Res.string.id_redeem),
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}
