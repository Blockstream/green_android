package com.blockstream.compose.screens.send

import com.blockstream.common.Parcelable
import android.view.WindowInsets.Side
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelize
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.SendConfirmViewModel
import com.blockstream.common.models.send.SendConfirmViewModelAbstract
import com.blockstream.common.models.send.SendConfirmViewModelPreview
import com.blockstream.common.models.transaction.TransactionViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmount
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.ScreenContainer
import com.blockstream.compose.components.SlideToUnlock
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.NoteBottomSheet
import com.blockstream.compose.sheets.VerifyTransactionBottomSheet
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import org.koin.core.parameter.parametersOf

@Parcelize
data class SendConfirmScreen(
    val greenWallet: GreenWallet,
    val accountAsset: AccountAsset?,
    val denomination: Denomination?
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SendConfirmViewModel> {
            parametersOf(greenWallet, accountAsset, denomination)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        SendConfirmScreen(viewModel = viewModel)
    }
}

@Composable
fun SendConfirmScreen(
    viewModel: SendConfirmViewModelAbstract
) {
    val look by viewModel.transactionConfirmLook.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()
    val onProgressDescription by viewModel.onProgressDescription.collectAsStateWithLifecycle()

    getNavigationResult<String>(NoteBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(SendConfirmViewModel.LocalEvents.SetNote(it))
    }

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current

    HandleSideEffect(viewModel) {
        when (it) {
            is SendConfirmViewModel.LocalSideEffects.Note -> {
                bottomSheetNavigator.show(
                    NoteBottomSheet(
                        greenWallet = viewModel.greenWallet,
                        note = it.note,
                        isLightning = viewModel.account.isLightning
                    )
                )
            }
            is SideEffects.Dismiss -> {
                bottomSheetNavigator.hide()
            }
        }
    }

    ScreenContainer(
        onProgress = onProgressSending,
        onProgressDescription = onProgressDescription,
        blurBackground = true
    ) {

        GreenColumn(
            padding = 0,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Banner(viewModel)


                look?.from?.also {
                    GreenAccountAsset(
                        accountAssetBalance = it.accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        title = stringResource(R.string.id_from),
                        withAsset = false
                    )
                }

                look?.to?.also {
                    GreenAccountAsset(
                        accountAssetBalance = it.accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        title = stringResource(R.string.id_to),
                        withAsset = false
                    )
                }

                look?.amount?.also {
                    GreenAmount(
                        title = stringResource(R.string.id_amount),
                        amount = it,
                        amountFiat = look?.amountFiat,
                    )
                }

                look?.utxos?.forEach {
                    GreenAmount(
                        title = stringResource(if(look?.isRedeposit == true) R.string.id_your_redeposit_address else R.string.id_sent_to),
                        amount = it.amount ?: "",
                        amountFiat = it.amountExchange,
                        assetId = it.assetId,
                        address = it.address,
                        session = viewModel.sessionOrNull,
                        showIcon = true
                    )
                }

                val note by viewModel.note.collectAsStateWithLifecycle()
                AnimatedVisibility(visible = note.isNotBlank()) {
                    GreenDataLayout(
                        title = stringResource(R.string.id_note),
                        withPadding = false
                    ) {
                        Row {
                            Text(
                                text = note, modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp)
                                    .padding(start = 16.dp)
                            )
                            IconButton(onClick = {
                                viewModel.postEvent(SendConfirmViewModel.LocalEvents.Note)
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.pencil_simple_line),
                                    contentDescription = "Edit",
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                    }
                }
            }

            Row {
                Column {
                    Text(
                        stringResource(R.string.id_network_fee),
                        color = whiteMedium,
                        style = labelLarge
                    )
                    look?.feeRate?.also {
                        Text(
                            text = it,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = whiteMedium,
                            style = bodySmall
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    look?.fee?.also {
                        Text(it, color = whiteHigh, style = labelLarge)
                    }
                    look?.feeFiat?.also {
                        Text(it, color = whiteMedium, style = labelMedium)
                    }
                }
            }
            look?.total?.also { total ->
                HorizontalDivider()

                Row {
                    Text(
                        stringResource(R.string.id_total_spent),
                        color = whiteHigh,
                        modifier = Modifier.weight(1f),
                        style = titleSmall
                    )
                    Column(horizontalAlignment = Alignment.End) {

                        Text(total, color = whiteHigh, style = titleSmall)

                        look?.totalFiat?.also {
                            Text(it, color = whiteMedium, style = labelLarge)
                        }
                    }
                }
            }

            val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

            SlideToUnlock(
                modifier = Modifier.padding(top = 8.dp),
                isLoading = onProgressSending,
                enabled = buttonEnabled,
                onSlideComplete = {
                    viewModel.postEvent(
                        CreateTransactionViewModelAbstract.LocalEvents.SignTransaction(
                            broadcastTransaction = true
                        )
                    )
                }
            )
        }
    }
}

@Composable
@Preview
fun SendConfirmScreenPreview() {
    GreenPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.preview())
    }
}

@Composable
@Preview
fun SendConfirmScreenExchangePreview() {
    GreenPreview {
        SendConfirmScreen(viewModel = SendConfirmViewModelPreview.previewAccountExchange())
    }
}
