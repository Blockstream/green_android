package com.blockstream.compose.screens.lightning

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount_to_receive
import blockstream_green.common.generated.resources.id_description
import blockstream_green.common.generated.resources.id_redeem
import com.blockstream.data.data.DenominatedValue
import com.blockstream.compose.components.AmountFieldAction
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.models.lightning.LnUrlWithdrawViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun LnUrlWithdrawScreen(
    viewModel: LnUrlWithdrawViewModelAbstract,
) {

    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, onProgressStyle = OnProgressStyle.Top) {

        GreenColumn(padding = 0) {
            Text(
                text = viewModel.redeemMessage,
                textAlign = TextAlign.Center,
                style = labelLarge,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 16.dp)
                    .fillMaxWidth()
            )

            val amountFieldState by viewModel.amountFieldState.collectAsStateWithLifecycle()
            val withdrawalLimits by viewModel.withdrawalLimits.collectAsStateWithLifecycle()

            LnUrlWithdrawAmountField(
                state = amountFieldState,
                onValueChange = viewModel.amount.onValueChange(),
                onAction = { action ->
                    when (action) {
                        AmountFieldAction.Clear -> viewModel.amount.value = ""
                        AmountFieldAction.SelectDenomination -> {
                            viewModel.postEvent(Events.SelectDenomination)
                        }
                        AmountFieldAction.Edit -> Unit
                    }
                },
                withdrawalLimits = withdrawalLimits,
                label = stringResource(Res.string.id_amount_to_receive),
            )

            val description by viewModel.description.collectAsStateWithLifecycle()
            GreenTextField(
                title = stringResource(Res.string.id_description),
                value = description,
                onValueChange = viewModel.description.onValueChange(),
                enabled = !onProgress,
            )

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
