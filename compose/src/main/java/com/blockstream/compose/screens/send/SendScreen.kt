package com.blockstream.compose.screens.send

import com.blockstream.common.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelize
import com.blockstream.common.AddressInputType
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.FeePriority
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.send.CreateTransactionViewModelAbstract
import com.blockstream.common.models.send.SendViewModel
import com.blockstream.common.models.send.SendViewModelAbstract
import com.blockstream.common.models.send.SendViewModelPreview
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenNetworkFee
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.components.ScreenContainer
import com.blockstream.compose.components.SlideToUnlock
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.AssetsAccountsBottomSheet
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_onError
import com.blockstream.compose.theme.md_theme_onErrorContainer
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.stringResourceId
import com.blockstream.compose.utils.toBitmap
import org.koin.core.parameter.parametersOf

@Parcelize
data class SendScreen(
    val greenWallet: GreenWallet,
    val address: String? = null,
    val addressInputType: AddressInputType? = null
) : Parcelable, Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SendViewModel> {
            parametersOf(greenWallet, address, addressInputType)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        SendScreen(viewModel = viewModel)
    }
}

@Composable
fun SendScreen(
    viewModel: SendViewModelAbstract
) {

    getNavigationResult<ScanResult>(CameraBottomSheet::class.resultKey).value?.also {
        viewModel.address.value = it.result
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetAddressInputType(AddressInputType.SCAN))
    }

    getNavigationResult<AccountAssetBalance>(AssetsAccountsBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(Events.SetAccountAsset(it.accountAsset))
    }

    getNavigationResult<FeePriority>(FeeRateBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetFeeRate(it))
    }

    getNavigationResult<DenominatedValue>(DenominationBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    var customFeeDialog by remember { mutableStateOf<String?>(null) }

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
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

    val errorAddress by viewModel.errorAddress.collectAsStateWithLifecycle()
    val accountAssetBalance by viewModel.accountAssetBalance.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()

    ScreenContainer(
        onProgress = onProgressSending,
        blurBackground = true,
        riveAnimation = R.raw.lightning_transaction
    ) {
        AnimatedVisibility(visible = onProgress && !onProgressSending, modifier = Modifier.align(Alignment.TopCenter)) {
            LinearProgressIndicator(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
        }

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

                val address by viewModel.address.collectAsStateWithLifecycle()
                GreenTextField(
                    title = stringResource(id = R.string.id_recipient_address),
                    value = address,
                    onValueChange = {
                        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetAddressInputType(AddressInputType.PASTE))
                        viewModel.address.value = it
                    },
                    singleLine = false,
                    maxLines = 4,
                    error = errorAddress,
                    onQrClick = {
                        bottomSheetNavigator.show(
                            CameraBottomSheet(
                                isDecodeContinuous = true,
                                parentScreenName = viewModel.screenName(),
                            )
                        )
                    }
                )


                val isAccountEdit by viewModel.isAccountEdit.collectAsStateWithLifecycle()
                AnimatedNullableVisibility(value = accountAssetBalance) {
                    GreenAccountAsset(
                        accountAssetBalance = it,
                        session = viewModel.sessionOrNull,
                        title = stringResource(if (accountAssetBalance?.account?.isLightning == true) R.string.id_lightning_account else R.string.id_account__asset),
                        withEditIcon = isAccountEdit,
                        onClick = if (isAccountEdit) {
                            {
                                viewModel.postEvent(SendViewModel.LocalEvents.ClickAssetsAccounts)
                            }
                        } else null
                    )
                }

                val amount by viewModel.amount.collectAsStateWithLifecycle()
                val amountHint by viewModel.amountHint.collectAsStateWithLifecycle()
                val amountExchange by viewModel.amountExchange.collectAsStateWithLifecycle()
                val denomination by viewModel.denomination.collectAsStateWithLifecycle()
                val errorAmount by viewModel.errorAmount.collectAsStateWithLifecycle()
                val isAmountLocked by viewModel.isAmountLocked.collectAsStateWithLifecycle()
                val isSendAll by viewModel.isSendAll.collectAsStateWithLifecycle()
                val supportsSendAll by viewModel.supportsSendAll.collectAsStateWithLifecycle()

                AnimatedNullableVisibility(value = accountAssetBalance) {
                    GreenAmountField(
                        value = amount,
                        onValueChange = {
                            viewModel.isSendAll.value = false
                            viewModel.amount.value = it
                        },
                        assetId = it.assetId,
                        session = viewModel.sessionOrNull,
                        isAmountLocked = isAmountLocked,
                        error = errorAmount,
                        denomination = denomination,
                        sendAll = isSendAll,
                        supportsSendAll = supportsSendAll,
                        onSendAllClick = {
                            viewModel.postEvent(SendViewModel.LocalEvents.ToggleIsSendAll)
                        },
                        footerContent = {
                            Row(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResourceId(id = amountHint ?: ""),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f),
                                    style = bodyMedium,
                                    color = whiteLow
                                )

                                Text(
                                    text = amountExchange,
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
                }

                val metadataDomain by viewModel.metadataDomain.collectAsStateWithLifecycle()
                AnimatedNullableVisibility(value = metadataDomain) {
                    Text(
                        text = stringResourceId(id = it),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        textAlign = TextAlign.Center,
                        style = labelLarge,
                        color = whiteHigh
                    )
                }

                val metadataImage by viewModel.metadataImage.collectAsStateWithLifecycle()
                AnimatedNullableVisibility(
                    value = metadataImage?.toBitmap(),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                val metadataDescription by viewModel.metadataDescription.collectAsStateWithLifecycle()
                AnimatedNullableVisibility(
                    value = metadataDescription,
                ) {
                    Text(
                        text = it,
                        style = bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            val errorGeneric by viewModel.errorGeneric.collectAsStateWithLifecycle()
            AnimatedNullableVisibility(value = errorGeneric) {
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

            val showFeeSelector by viewModel.showFeeSelector.collectAsStateWithLifecycle()
            val feePriority by viewModel.feePriority.collectAsStateWithLifecycle()
            AnimatedVisibility(
                visible = showFeeSelector
            ) {
                GreenNetworkFee(
                    feePriority = feePriority, onClick = { onIconClicked ->
                        viewModel.postEvent(
                            CreateTransactionViewModelAbstract.LocalEvents.ClickFeePriority(
                                showCustomFeeRateDialog = onIconClicked
                            )
                        )
                    }
                )
            }

            AnimatedNullableVisibility(value = accountAssetBalance) {
                val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

                if (it.account.isLightning) {
                    SlideToUnlock(
                        isLoading = onProgressSending,
                        enabled = buttonEnabled,
                        onSlideComplete = {
                            viewModel.postEvent(SendViewModel.LocalEvents.SendLightningTransaction)
                        }
                    )
                } else {
                    GreenButton(
                        text = stringResource(id = R.string.id_next),
                        enabled = buttonEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(Events.Continue)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun SendScreenPreview() {
    GreenPreview {
        SendScreen(viewModel = SendViewModelPreview.preview())
    }
}
