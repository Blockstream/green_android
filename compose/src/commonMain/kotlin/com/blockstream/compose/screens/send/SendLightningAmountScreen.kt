package com.blockstream.compose.screens.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_available
import blockstream_green.common.generated.resources.id_next
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.send.SendLightningAmountViewModelAbstract
import com.blockstream.compose.models.send.SendLightningAmountViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.data.data.DenominatedValue
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendLightningAmountScreen(
    viewModel: SendLightningAmountViewModelAbstract,
) {
    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val amountExchange by viewModel.amountExchange.collectAsStateWithLifecycle()
    val denomination by viewModel.denomination.collectAsStateWithLifecycle()
    val accountAssetBalance by viewModel.accountAssetBalance.collectAsStateWithLifecycle()
    val isValid by viewModel.isValid.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        withImePadding = true,
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            GreenColumn(
                padding = 0,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GreenAmountField(
                    value = amount,
                    onValueChange = { viewModel.amount.value = it },
                    secondaryValue = amountExchange,
                    showTitle = false,
                    assetId = accountAssetBalance?.assetId,
                    session = viewModel.sessionOrNull,
                    denomination = denomination,
                    availableBalance = null,
                    helperText = error,
                    supportsSendAll = false,
                    focusRequester = focusRequester,
                    onDenominationClick = { viewModel.postEvent(Events.SelectDenomination) },
                )

                accountAssetBalance?.balance?.let {
                    Text(
                        text = "${stringResource(Res.string.id_available)} $it",
                        style = bodyMedium,
                        color = whiteMedium,
                    )
                }
            }

            GreenButton(
                text = stringResource(Res.string.id_next),
                enabled = isValid,
                size = GreenButtonSize.BIG,
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.postEvent(Events.Continue) },
            )
        }
    }
}

@Composable
@Preview
fun SendLightningAmountScreenPreview() {
    GreenPreview {
        SendLightningAmountScreen(viewModel = SendLightningAmountViewModelPreview.preview())
    }
}
