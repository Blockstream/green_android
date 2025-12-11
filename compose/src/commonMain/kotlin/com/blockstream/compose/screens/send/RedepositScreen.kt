package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_fee_rate
import blockstream_green.common.generated.resources.id_next
import blockstream_green.common.generated.resources.id_redeposit
import blockstream_green.common.generated.resources.id_set_custom_fee_rate
import blockstream_green.common.generated.resources.id_total_spent
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.FeePriority
import com.blockstream.common.events.Events
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.RedepositViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.common.utils.stringResourceFromId
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenNetworkFee
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_onError
import com.blockstream.compose.theme.md_theme_onErrorContainer
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.getResult
import org.jetbrains.compose.resources.stringResource

@Composable
fun RedepositScreen(
    viewModel: RedepositViewModelAbstract
) {
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

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    SetupScreen(
        viewModel = viewModel,
        onProgressStyle = OnProgressStyle.Full(bluBackground = true), sideEffectsHandler = {
            if (it is CreateTransactionViewModelAbstract.LocalSideEffects.ShowCustomFeeRate) {
                customFeeDialog = it.feeRate.toString()
            }
        }
    ) {
        AnimatedVisibility(visible = onProgress) {
            LinearProgressIndicator(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
        }

        Column {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                GreenColumn(padding = 0, space = 8) {
                    val accountAssetBalance by viewModel.accountAssetBalance.collectAsStateWithLifecycle()

                    GreenAccountAsset(
                        accountAssetBalance = accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        title = stringResource(Res.string.id_redeposit),
                        withEditIcon = false,
                    )
                }

                val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()
                GreenNetworkFee(
                    feePriority = feePriority, onClick = { onIconClicked ->
                        viewModel.postEvent(
                            CreateTransactionViewModelAbstract.LocalEvents.ClickFeePriority(
                                showCustomFeeRateDialog = onIconClicked
                            )
                        )
                    }
                )

                val error by viewModel.error.collectAsStateWithLifecycle()
                AnimatedNullableVisibility(value = error) {
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

            val showFeeSelector by viewModel.showFeeSelector.collectAsStateWithLifecycle()
            val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()
            AnimatedVisibility(visible = showFeeSelector) {
                Row {
                    Text(
                        stringResource(Res.string.id_total_spent),
                        color = whiteHigh,
                        modifier = Modifier.weight(1f),
                        style = titleSmall
                    )
                    Column(horizontalAlignment = Alignment.End) {

                        feePriority.fee?.also {
                            Text(it, color = whiteHigh, style = titleSmall)
                        }
                        feePriority.feeFiat?.also {
                            Text(it, color = whiteMedium, style = labelLarge)
                        }
                    }
                }
            }

            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
            GreenButton(
                text = stringResource(Res.string.id_next),
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.postEvent(Events.Continue)
            }
        }
    }
}
