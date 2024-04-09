package com.blockstream.compose.screens.send

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.send.BumpViewModel
import com.blockstream.common.models.send.BumpViewModelAbstract
import com.blockstream.common.models.send.BumpViewModelPreview
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.SweepViewModel
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAccount
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.GreenNetworkFee
import com.blockstream.compose.components.SlideToUnlock
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_onError
import com.blockstream.compose.theme.md_theme_onErrorContainer
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import org.koin.core.parameter.parametersOf

@Parcelize
data class BumpScreen(
    val greenWallet: GreenWallet,
    val accountAsset: AccountAsset,
    val transaction: String
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<BumpViewModel>() {
            parametersOf(greenWallet, accountAsset, transaction)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        BumpScreen(viewModel = viewModel)
    }
}

@Composable
fun BumpScreen(
    viewModel: BumpViewModelAbstract
) {
    getNavigationResult<FeePriority>(FeeRateBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetFeeRate(it))
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

    val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()

    GreenColumn {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()

            accountAsset?.also {
                GreenAccount(
                    title = stringResource(R.string.id_account),
                    account = it.account,
                    session = viewModel.sessionOrNull,
                    withEditIcon = false
                )
            }

            val address by viewModel.address.collectAsStateWithLifecycle()
            AnimatedNullableVisibility(value = address) {
                GreenDataLayout(title = stringResource(id = R.string.id_address)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {
                        GreenAddress(address = it)
                    }
                }
            }

            val amount by viewModel.amount.collectAsStateWithLifecycle()
            val amountFiat by viewModel.amountFiat.collectAsStateWithLifecycle()

            AnimatedNullableVisibility(value = amount) {
                GreenDataLayout(title = stringResource(id = R.string.id_amount)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {

                        SelectionContainer {
                            Text(text = it, style = titleLarge)
                        }

                        amountFiat?.also { fiat ->
                            SelectionContainer {
                                Text(text = fiat, style = bodyLarge)
                            }
                        }
                    }
                }
            }


            GreenNetworkFee(
                feePriority = feePriority, onClick = { onIconClicked ->
                    viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.ClickFeePriority(showCustomFeeRateDialog = onIconClicked))
                }
            )
        }

        val total by viewModel.total.collectAsStateWithLifecycle()
        val totalFiat by viewModel.totalFiat.collectAsStateWithLifecycle()
        val oldFee by viewModel.oldFee.collectAsStateWithLifecycle()
        val oldFeeFiat by viewModel.oldFeeFiat.collectAsStateWithLifecycle()
        val oldFeeRate by viewModel.oldFeeRate.collectAsStateWithLifecycle()
        GreenColumn(
            padding = 0,
            space = 4,
            modifier = Modifier
                .padding(horizontal = 4.dp)
        ) {
            // Old Fee
            AnimatedNullableVisibility(value = oldFee) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(stringResource(id = R.string.id_old_fee), style = bodyMedium, color = whiteMedium)
                        Text(oldFeeRate ?: "", style = bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough
                        ), color = red)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(oldFee ?: "", style = bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough
                        ), color = red)
                        Text(oldFeeFiat ?: "", style = bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough
                        ), color = red)
                    }
                }
            }


            // New Fee
            AnimatedNullableVisibility(value = feePriority.fee) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(stringResource(id = R.string.id_new_fee), style = bodyMedium, color = whiteMedium)
                        Text(feePriority.feeRate ?: "", style = bodyMedium, color = green)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(feePriority.fee ?: "", style = bodyMedium, color = green)
                        Text(feePriority.feeFiat ?: "", style = bodySmall, color = green)
                    }
                }
            }

            AnimatedNullableVisibility(value = total) {
                GreenColumn(padding = 0, space = 4) {
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {
                        Text(
                            stringResource(id = R.string.id_total),
                            style = labelLarge,
                            color = whiteHigh
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(total ?: "", style = labelLarge)
                            Text(totalFiat ?: "", style = bodyMedium, color = whiteMedium)
                        }
                    }
                }
            }
        }

        AnimatedNullableVisibility(value = error.takeIf { !listOf("id_invalid_replacement_fee_rate", "id_fee_rate_is_below_minimum").contains(it) }) {
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

        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        SlideToUnlock(isLoading = onProgress, enabled = buttonEnabled, onSlideComplete = {
             viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SignTransaction())
        })
    }
}

@Composable
@Preview
fun BumpScreenPreview() {
    GreenPreview {
        BumpScreen(viewModel = BumpViewModelPreview.preview())
    }
}
