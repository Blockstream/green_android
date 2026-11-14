package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account__asset
import blockstream_green.common.generated.resources.id_all_coins
import blockstream_green.common.generated.resources.id_available
import blockstream_green.common.generated.resources.id_coin
import blockstream_green.common.generated.resources.id_coins
import blockstream_green.common.generated.resources.id_comment
import blockstream_green.common.generated.resources.id_description
import blockstream_green.common.generated.resources.id_fee_rate
import blockstream_green.common.generated.resources.id_lightning_account
import blockstream_green.common.generated.resources.id_next
import blockstream_green.common.generated.resources.id_recipient
import blockstream_green.common.generated.resources.id_set_custom_fee_rate
import blockstream_green.common.generated.resources.pencil_simple_line
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.FeePriority
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.utils.DecimalFormat
import com.blockstream.data.utils.FEE_RATE_DECIMALS
import com.blockstream.data.utils.userNumberFormat
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.GreenNetworkFee
import com.blockstream.compose.components.GreenTextField
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.send.CoinSelectionResult
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.models.send.SendViewModel
import com.blockstream.compose.models.send.SendViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_onError
import com.blockstream.compose.theme.md_theme_onErrorContainer
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.toPainter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SendScreen(
    viewModel: SendViewModelAbstract
) {
    NavigateDestinations.AssetsAccounts.getResult<AccountAssetBalance> {
        viewModel.postEvent(Events.SetAccountAsset(it.accountAsset))
    }

    NavigateDestinations.FeeRate.getResult<FeePriority> {
        viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SetFeeRate(it))
    }

    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    NavigateDestinations.Note.getResult<String> {
        viewModel.note.value = it
    }

    NavigateDestinations.CoinSelection.getResult<CoinSelectionResult> {
        viewModel.postEvent(SendViewModel.LocalEvents.SetSelectedUtxos(it))
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

    val accountAssetBalance by viewModel.accountAssetBalance.collectAsStateWithLifecycle()
    val canSelectCoins by viewModel.canSelectCoins.collectAsStateWithLifecycle()
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        onProgressStyle = if (onProgressSending) OnProgressStyle.Full(bluBackground = true) else OnProgressStyle.Top,
        sideEffectsHandler = {
            if (it is CreateTransactionViewModelAbstract.LocalSideEffects.ShowCustomFeeRate) {
                customFeeDialog = it.feeRate.toString()
            }
        }
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            val selectedUtxosCount by viewModel.selectedUtxosCount.collectAsStateWithLifecycle()

            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Banner(viewModel)

                GreenTextField(
                    title = stringResource(Res.string.id_recipient),
                    value = viewModel.address,
                    onValueChange = { },
                    singleLine = false,
                    enabled = false,
                    maxLines = 4,
                )

                AnimatedNullableVisibility(value = accountAssetBalance) {
                    GreenAccountAsset(
                        accountAssetBalance = it,
                        session = viewModel.sessionOrNull,
                        title = stringResource(if (accountAssetBalance?.account?.isLightning == true) Res.string.id_lightning_account else Res.string.id_account__asset),
                    )
                }

                val amount by viewModel.amount.collectAsStateWithLifecycle()
                val amountHint by viewModel.amountHint.collectAsStateWithLifecycle()
                val amountExchange by viewModel.amountExchange.collectAsStateWithLifecycle()
                val showAmount by viewModel.showAmount.collectAsStateWithLifecycle()
                val denomination by viewModel.denomination.collectAsStateWithLifecycle()
                val errorAmount by viewModel.errorAmount.collectAsStateWithLifecycle()
                val availableBalance by viewModel.availableBalance.collectAsStateWithLifecycle()
                val isAmountLocked by viewModel.isAmountLocked.collectAsStateWithLifecycle()
                val isSendAll by viewModel.isSendAll.collectAsStateWithLifecycle()
                val supportsSendAll = viewModel.supportsSendAll
                val showDenominationSelector by viewModel.showDenominationSelector.collectAsStateWithLifecycle()

                AnimatedVisibility(visible = showAmount && accountAssetBalance != null) {
                    GreenAmountField(
                        value = amount,
                        onValueChange = {
                            viewModel.isSendAll.value = false
                            viewModel.amount.value = it
                        },
                        secondaryValue = amountExchange,
                        assetId = accountAssetBalance?.assetId,
                        session = viewModel.sessionOrNull,
                        isAmountLocked = isAmountLocked,
                        helperText = errorAmount,
                        denomination = denomination,
                        sendAll = isSendAll,
                        supportsSendAll = supportsSendAll && !canSelectCoins,
                        availableBalance = availableBalance.takeIf { !canSelectCoins },
                        isMaxPayable = accountAssetBalance?.account?.isLightning == true,
                        onSendAllClick = {
                            viewModel.postEvent(SendViewModel.LocalEvents.ToggleIsSendAll)
                        },
                        footerContent = {
                            GreenColumn(padding = 0, space = 0, modifier = Modifier.fillMaxWidth()) {
                                if (!amountHint.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = amountHint ?: "",
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.weight(1f),
                                            style = bodyMedium,
                                            color = whiteLow
                                        )
                                    }
                                }

                                if (canSelectCoins) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.small)
                                                .clickable(enabled = !isAmountLocked) {
                                                    viewModel.postEvent(SendViewModel.LocalEvents.ToggleIsSendAll)
                                                }
                                                .heightIn(min = 40.dp)
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.id_available),
                                                style = bodyMedium,
                                                color = whiteMedium
                                            )
                                            Text(
                                                text = availableBalance ?: "",
                                                style = bodyMedium,
                                                color = if (isAmountLocked) whiteMedium else green
                                            )
                                            AnimatedVisibility(
                                                visible = isSendAll,
                                                enter = fadeIn() + scaleIn(),
                                                exit = fadeOut() + scaleOut()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = green,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.small)
                                                .clickable {
                                                    viewModel.postEvent(SendViewModel.LocalEvents.OpenCoinSelection)
                                                }
                                                .heightIn(min = 40.dp)
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedUtxosCount > 0) {
                                                    "$selectedUtxosCount ${stringResource(if (selectedUtxosCount == 1) Res.string.id_coin else Res.string.id_coins)}"
                                                } else {
                                                    stringResource(Res.string.id_all_coins)
                                                },
                                                style = bodyMedium,
                                                color = whiteMedium
                                            )
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.CaretRight,
                                                contentDescription = null,
                                                tint = whiteMedium,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onDenominationClick = if (showDenominationSelector) {
                            { viewModel.postEvent(Events.SelectDenomination) }
                        } else null
                    )
                }

                val description by viewModel.description.collectAsStateWithLifecycle() // Bolt11
                val comment by viewModel.note.collectAsStateWithLifecycle() // LNURL
                val commentOrDescription = description ?: comment
                val isNoteEditable by viewModel.isNoteEditable.collectAsStateWithLifecycle()
                AnimatedVisibility(visible = commentOrDescription.isNotBlank()) {
                    GreenDataLayout(
                        title = stringResource(if (description != null) Res.string.id_description else Res.string.id_comment),
                        withPadding = false
                    ) {
                        Row {
                            Text(
                                text = commentOrDescription, modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp)
                                    .padding(start = 16.dp)
                            )
                            if (isNoteEditable) {
                                IconButton(onClick = {
                                    viewModel.postEvent(SendViewModel.LocalEvents.Note)
                                }) {
                                    Icon(
                                        painter = painterResource(Res.drawable.pencil_simple_line),
                                        contentDescription = "Edit",
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    )
                                }
                            }
                        }
                    }
                }

                val metadataDomain by viewModel.metadataDomain.collectAsStateWithLifecycle()
                AnimatedNullableVisibility(value = metadataDomain) {
                    Text(
                        text = it,
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
                    value = metadataImage?.toPainter(),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                ) {
                    Image(
                        painter = it,
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
                        Text(text = it)
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
                val isValid by viewModel.isValid.collectAsStateWithLifecycle()

                GreenButton(
                    text = stringResource(Res.string.id_next),
                    enabled = isValid,
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.postEvent(Events.Continue)
                }
            }
        }
    }
}
