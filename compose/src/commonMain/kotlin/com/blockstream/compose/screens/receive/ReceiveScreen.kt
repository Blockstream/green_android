package com.blockstream.compose.screens.receive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.get
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrows_counter_clockwise
import blockstream_green.common.generated.resources.id_account__asset
import blockstream_green.common.generated.resources.id_account_address
import blockstream_green.common.generated.resources.id_address
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_amount_to_receive
import blockstream_green.common.generated.resources.id_confirm
import blockstream_green.common.generated.resources.id_description
import blockstream_green.common.generated.resources.id_expiration
import blockstream_green.common.generated.resources.id_ledger_supports_a_limited_set
import blockstream_green.common.generated.resources.id_lightning_invoice
import blockstream_green.common.generated.resources.id_onchain_address
import blockstream_green.common.generated.resources.id_please_verify_that_the_address
import blockstream_green.common.generated.resources.id_qr_code
import blockstream_green.common.generated.resources.id_request_amount
import blockstream_green.common.generated.resources.id_share
import blockstream_green.common.generated.resources.id_show_lightning_invoice
import blockstream_green.common.generated.resources.id_show_onchain_address
import blockstream_green.common.generated.resources.id_verify_on_device
import blockstream_green.common.generated.resources.info
import blockstream_green.common.generated.resources.seal_check
import blockstream_green.common.generated.resources.share_network
import blockstream_green.common.generated.resources.warning
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.MenuEntry
import com.blockstream.common.data.MenuEntryList
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.receive.ReceiveViewModel
import com.blockstream.common.models.receive.ReceiveViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.orange
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AlphaPulse
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenGradient
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.LocalNavigator
import com.blockstream.ui.navigation.bottomsheet.BottomSheetNavigator
import com.blockstream.ui.navigation.getResult
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModelAbstract
) {

    NavigateDestinations.ReviewAddAccount.getResult<AccountAsset> {
        viewModel.postEvent(Events.SetAccountAsset(it))
    }

    NavigateDestinations.ChooseAssetAccounts.getResult<AccountAsset> {
        viewModel.accountAsset.value = it
    }

    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    NavigateDestinations.Note.getResult<String> {
        viewModel.postEvent(ReceiveViewModel.LocalEvents.SetNote(it))
    }

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()
    val showRequestAmount by viewModel.showAmount.collectAsStateWithLifecycle()
    val receiveAddress by viewModel.receiveAddress.collectAsStateWithLifecycle()
    val receiveAddressUri by viewModel.receiveAddressUri.collectAsStateWithLifecycle()
    val onchainSwapMessage by viewModel.onchainSwapMessage.collectAsStateWithLifecycle()
    val denomination by viewModel.denomination.collectAsStateWithLifecycle()
    val showLightningOnChainAddress by viewModel.showLightningOnChainAddress.collectAsStateWithLifecycle()
    val showLedgerAssetWarning by viewModel.showLedgerAssetWarning.collectAsStateWithLifecycle()
    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val bottomSheetNavigator = LocalNavigator.current.navigatorProvider[BottomSheetNavigator::class]

    val platformManager = rememberPlatformManager()

    NavigateDestinations.Menu.getResult<Int> {
        scope.launch {
            val qrCode : Painter = QrCodePainter(
                data = receiveAddressUri ?: "",
            )

            viewModel.postEvent(
                if (it == 0) ReceiveViewModel.LocalEvents.ShareAddress else ReceiveViewModel.LocalEvents.ShareQR(
                    qrCode.toByteArray(800, 800).let {
                        platformManager.processQr(it, receiveAddressUri ?: "")
                    }
                )
            )
        }
    }

    val focusRequester = remember { FocusRequester() }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val maxReceiveAmount by viewModel.maxReceiveAmount.collectAsStateWithLifecycle()
    val amountExchange by viewModel.amountExchange.collectAsStateWithLifecycle()
    val amountError by viewModel.amountError.collectAsStateWithLifecycle()
    val liquidityFee by viewModel.liquidityFee.collectAsStateWithLifecycle()
    val showRecoveryConfirmation by viewModel.showRecoveryConfirmation.collectAsStateWithLifecycle()

    LaunchedEffect(showRequestAmount){
        if(showRequestAmount && amount.isBlank()){
            focusRequester.requestFocus()
        }
    }

    SetupScreen(viewModel = viewModel, withPadding = false, onProgressStyle = OnProgressStyle.Disabled, sideEffectsHandler = {
        when (it) {
            is SideEffects.Dismiss -> {
                bottomSheetNavigator.popBackStack()
            }
        }
    }) {

        Box(modifier = Modifier.weight(1f)) {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {

                if (showRecoveryConfirmation) {
                    GreenAlert(
                        alertType = AlertType.RecoveryIsUnconfirmed(withCloseButton = true),
                        viewModel = viewModel
                    )
                }

                Column {
                    accountAsset?.also {
                        AnimatedVisibility(visible = showLedgerAssetWarning) {
                            GreenCard(padding = 8, colors = CardDefaults.elevatedCardColors(
                                containerColor = orange
                            ), onClick = {
                                viewModel.postEvent(ReceiveViewModel.LocalEvents.ClickLedgerSupportedAssets)
                            }
                            ) {
                                GreenRow(
                                    padding = 0,
                                    space = 6,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painterResource(Res.drawable.warning),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = whiteMedium
                                    )
                                    Text(
                                        stringResource(Res.string.id_ledger_supports_a_limited_set),
                                        style = bodyMedium,
                                        color = whiteMedium,
                                    )
                                }
                            }
                        }

                        GreenAccountAsset(
                            accountAssetBalance = it.accountAssetBalance,
                            session = viewModel.sessionOrNull,
                            title = stringResource(Res.string.id_account__asset),
                            withEditIcon = true
                        ) {
                            viewModel.postEvent(NavigateDestinations.ChooseAssetAccounts(greenWallet = viewModel.greenWallet))
                        }
                    }
                }

                AnimatedVisibility(visible = accountAsset?.account?.isLightning == true && !showLightningOnChainAddress || showRequestAmount) {

                    GreenColumn(padding = 0, space = 8) {

                        GreenAmountField(
                            value = amount,
                            onValueChange = viewModel.amount.onValueChange(),
                            secondaryValue = amountExchange,
                            assetId = viewModel.accountAsset.value?.assetId,
                            session = viewModel.sessionOrNull,
                            title = if (accountAsset?.account?.isLightning == false) stringResource(
                                Res.string.id_request_amount
                            ) else stringResource(Res.string.id_amount),
                            helperText = amountError,
                            enabled = !onProgress,
                            denomination = denomination,
                            focusRequester = focusRequester,
                            isReadyOnly = accountAsset?.account?.isLightning == true && receiveAddress != null,
                            onEditClick = {
                                viewModel.postEvent(ReceiveViewModel.LocalEvents.ClearLightningInvoice)
                            },
                            footerContent = {
                                if (accountAsset?.account?.isLightning == true) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = maxReceiveAmount,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.weight(1f),
                                            style = bodyMedium,
                                            color = whiteLow
                                        )
                                    }
                                }
                            },
                            onDenominationClick = {
                                viewModel.postEvent(Events.SelectDenomination)
                            })

                        AnimatedNullableVisibility(liquidityFee) {
                            GreenCard(
                                padding = 0, colors = CardDefaults.elevatedCardColors(
                                    containerColor = green20
                                )
                            ) {
                                Column {
                                    Text(
                                        it,
                                        style = bodyMedium,
                                        color = whiteMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                            .padding(top = 8.dp)
                                    )
                                    LearnMoreButton(color = whiteMedium) {
                                        viewModel.postEvent(ReceiveViewModel.LocalEvents.ClickFundingFeesLearnMore)
                                    }
                                }
                            }
                        }
                    }
                }

                if (receiveAddress.isNotBlank() || accountAsset?.account?.isLightning == false) {

                    Column {

                        val addressTitle = if (accountAsset?.account?.isLightning == true) {
                            if (showLightningOnChainAddress) {
                                Res.string.id_onchain_address
                            } else {
                                Res.string.id_lightning_invoice
                            }
                        } else {
                            Res.string.id_account_address
                        }

                        Text(
                            stringResource(addressTitle),
                            style = labelMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )

                        Card {

                            Box(modifier = Modifier.padding(bottom = 8.dp)) {

                                if (accountAsset?.account?.isLightning == false) {
                                    IconButton(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 2.dp, end = 2.dp),
                                        onClick = {
                                            viewModel.postEvent(ReceiveViewModel.LocalEvents.GenerateNewAddress)
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.arrows_counter_clockwise),
                                            contentDescription = "Refresh"
                                        )
                                    }
                                }

                                Column {
                                    GreenQR(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 48.dp)
                                            .padding(top = 48.dp),
                                        onQrClick = {
                                            viewModel.postEvent(ReceiveViewModel.LocalEvents.CopyAddress)
                                        },
                                        data = receiveAddressUri,
                                    )

                                    GreenColumn(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {

                                        GreenAddress(
                                            address = receiveAddress ?: "",
                                            textAlign = TextAlign.Center,
                                            showCopyIcon = true,
                                            maxLines = if (accountAsset?.account?.isLightning == true && !showLightningOnChainAddress) 1 else 6,
                                            onCopyClick = {
                                                viewModel.postEvent(ReceiveViewModel.LocalEvents.CopyAddress)
                                            }
                                        )

                                        if (accountAsset?.account?.isLightning == true && showLightningOnChainAddress && onchainSwapMessage != null) {
                                            AlphaPulse {
                                                GreenCard(
                                                    padding = 8,
                                                    colors = CardDefaults.elevatedCardColors(
                                                        containerColor = green20
                                                    )
                                                ) {
                                                    Text(
                                                        onchainSwapMessage ?: "",
                                                        style = bodyMedium,
                                                        color = whiteMedium,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }

                                        val showVerifyOnDevice by viewModel.showVerifyOnDevice.collectAsStateWithLifecycle()
                                        AnimatedVisibility(visible = showVerifyOnDevice) {
                                            Column {
                                                GreenButton(
                                                    text = stringResource(Res.string.id_verify_on_device),
                                                    type = GreenButtonType.OUTLINE,
                                                    color = GreenButtonColor.GREENER,
                                                    icon = painterResource(Res.drawable.seal_check),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = !onProgress
                                                ) {
                                                    viewModel.postEvent(ReceiveViewModel.LocalEvents.VerifyOnDevice)
                                                }

                                                Text(
                                                    text = stringResource(Res.string.id_please_verify_that_the_address),
                                                    style = bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GreenColumn(padding = 0) {
                    val invoiceAmountToReceive by viewModel.invoiceAmountToReceive.collectAsStateWithLifecycle()
                    val invoiceAmountToReceiveFiat by viewModel.invoiceAmountToReceiveFiat.collectAsStateWithLifecycle()

                    invoiceAmountToReceive?.also {
                        Column {
                            Text(
                                stringResource(Res.string.id_amount_to_receive),
                                style = labelMedium,
                                color = whiteMedium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    it,
                                    style = titleSmall,
                                    color = whiteHigh,
                                    modifier = Modifier.weight(1f)
                                )
                                invoiceAmountToReceiveFiat?.also {
                                    Text(text = it, style = labelMedium, color = whiteLow)
                                }
                            }
                        }
                    }

                    val invoiceDescription by viewModel.invoiceDescription.collectAsStateWithLifecycle()
                    invoiceDescription?.also {
                        Column {
                            Text(
                                stringResource(Res.string.id_description),
                                style = labelMedium,
                                color = whiteMedium
                            )

                            Text(
                                it,
                                style = bodyLarge,
                                color = whiteHigh,
                            )
                        }
                    }

                    val invoiceExpiration by viewModel.invoiceExpiration.collectAsStateWithLifecycle()
                    invoiceExpiration?.also {
                        Column {
                            Text(
                                stringResource(Res.string.id_expiration),
                                style = labelMedium,
                                color = whiteMedium
                            )

                            Text(
                                it,
                                style = bodyLarge,
                                color = whiteHigh,
                            )
                        }
                    }
                }
            }

            GreenGradient(modifier = Modifier.align(Alignment.BottomCenter), size = 24)
        }

        Box {
            GreenColumn(
                space = 4,
                padding = 0,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp)
            ) {
                AnimatedVisibility(visible = receiveAddress.isNotBlank()) {
                    GreenButton(
                        text = stringResource(Res.string.id_share),
                        icon = painterResource(Res.drawable.share_network),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !onProgress
                    ) {
                        scope.launch {
                            viewModel.postEvent(
                                NavigateDestinations.Menu(
                                    title = getString(Res.string.id_share),
                                    entries = MenuEntryList(listOf(
                                        MenuEntry(
                                            title = getString(Res.string.id_address),
                                            iconRes = "text_aa"
                                        ),
                                        MenuEntry(
                                            title = getString(Res.string.id_qr_code),
                                            iconRes = "qr_code"
                                        ),
                                    ))
                                )
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = accountAsset?.account?.isLightning == true && !showLightningOnChainAddress && receiveAddress.isNullOrBlank()) {
                    GreenButton(
                        text = stringResource(Res.string.id_confirm),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = buttonEnabled
                    ) {
                        viewModel.postEvent(ReceiveViewModel.LocalEvents.CreateInvoice)
                    }
                }

                AnimatedVisibility(visible = accountAsset?.account?.isLightning == true) {
                    GreenButton(
                        text = stringResource(if (showLightningOnChainAddress) Res.string.id_show_lightning_invoice else Res.string.id_show_onchain_address),
                        modifier = Modifier.fillMaxWidth(),
                        type = GreenButtonType.TEXT,
                        color = GreenButtonColor.WHITE,
                        icon = painterResource(Res.drawable.info),
                        size = GreenButtonSize.SMALL,
                        enabled = !onProgress
                    ) {
                        viewModel.postEvent(ReceiveViewModel.LocalEvents.ToggleLightning)
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = onProgress,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}