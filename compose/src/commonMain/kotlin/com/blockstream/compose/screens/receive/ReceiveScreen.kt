package com.blockstream.compose.screens.receive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import blockstream_green.common.generated.resources.id_account_address
import blockstream_green.common.generated.resources.id_account_type_2fa_protected
import blockstream_green.common.generated.resources.id_account_type_amp
import blockstream_green.common.generated.resources.id_account_type_standard
import blockstream_green.common.generated.resources.id_address
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_asset
import blockstream_green.common.generated.resources.id_confirm
import blockstream_green.common.generated.resources.id_create_new_account
import blockstream_green.common.generated.resources.id_ledger_supports_a_limited_set
import blockstream_green.common.generated.resources.id_payer_sends
import blockstream_green.common.generated.resources.id_please_verify_that_the_address
import blockstream_green.common.generated.resources.id_qr_code
import blockstream_green.common.generated.resources.id_request_amount
import blockstream_green.common.generated.resources.id_share
import blockstream_green.common.generated.resources.id_verify_on_device
import blockstream_green.common.generated.resources.id_you_will_receive_liquid_bitcoin
import blockstream_green.common.generated.resources.warning
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.SealCheck
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.MenuEntry
import com.blockstream.common.data.MenuEntryList
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.compose.components.GreenAddress
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAmountField
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenGradient
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.models.receive.ReceiveViewModel
import com.blockstream.compose.models.receive.ReceiveViewModelAbstract
import com.blockstream.compose.navigation.LocalNavigator
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.bottomsheet.BottomSheetNavigator
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.screens.receive.components.LightningReadyBadge
import com.blockstream.compose.sheets.LightningInvoiceBottomSheet
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.orange
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.appTestTag
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModelAbstract
) {
    NavigateDestinations.Denomination.getResult<DenominatedValue> {
        viewModel.postEvent(Events.SetDenominatedValue(it))
    }

    NavigateDestinations.Note.getResult<String> {
        viewModel.postEvent(ReceiveViewModel.LocalEvents.SetNote(it))
    }

    NavigateDestinations.DeviceScan.getResult<GreenWallet> {
        viewModel.executePendingAction()
    }

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val asset by viewModel.asset.collectAsStateWithLifecycle()
    val accountAsset by viewModel.accountAsset.collectAsStateWithLifecycle()
    val showRequestAmount by viewModel.showAmount.collectAsStateWithLifecycle()
    val receiveAddress by viewModel.receiveAddress.collectAsStateWithLifecycle()
    val receiveAddressUri by viewModel.receiveAddressUri.collectAsStateWithLifecycle()
    val denomination by viewModel.denomination.collectAsStateWithLifecycle()
    val showLightningOnChainAddress by viewModel.showLightningOnChainAddress.collectAsStateWithLifecycle()
    val showLedgerAssetWarning by viewModel.showLedgerAssetWarning.collectAsStateWithLifecycle()
    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
    val isReverseSubmarineSwap by viewModel.isReverseSubmarineSwap.collectAsStateWithLifecycle()
    val showSwap by viewModel.showSwap.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val bottomSheetNavigator = LocalNavigator.current.navigatorProvider[BottomSheetNavigator::class]

    val platformManager = rememberPlatformManager()

    NavigateDestinations.Menu.getResult<Int> {
        if (it == 0) {
            scope.launch {
                viewModel.postEvent(ReceiveViewModel.LocalEvents.ShareAddress)
            }
        } else {
            scope.launch {
                runCatching {
                    val qrCode: Painter = QrCodePainter(
                        data = receiveAddressUri ?: "",
                    )

                    val data = qrCode.toByteArray(800, 800).let { platformManager.processQr(it, receiveAddressUri ?: "") }

                    viewModel.postEvent(event = ReceiveViewModel.LocalEvents.ShareQR(data = data))
                }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val receiveAmountData by viewModel.receiveAmountData.collectAsStateWithLifecycle()
    val showRecoveryConfirmation by viewModel.showRecoveryConfirmation.collectAsStateWithLifecycle()

    var showLightningInvoiceBottomSheet by remember { mutableStateOf(false) }

    val isLightningOrSwap = (accountAsset?.account?.isLightning == true && !showLightningOnChainAddress) || isReverseSubmarineSwap

    LaunchedEffect(receiveAddress, isLightningOrSwap) {
        if (isLightningOrSwap && receiveAddress.isNotBlank()) {
            showLightningInvoiceBottomSheet = true
        }
    }

    LaunchedEffect(showRequestAmount, isReverseSubmarineSwap, receiveAddress) {
        if ((showRequestAmount || (isReverseSubmarineSwap && receiveAddress == null)) && amount.isBlank()) {
            focusRequester.requestFocus()
        }
    }

    if (showLightningInvoiceBottomSheet) {
        LightningInvoiceBottomSheet(
            viewModel = viewModel,
            onDismissRequest = {
                showLightningInvoiceBottomSheet = false
                viewModel.postEvent(ReceiveViewModel.LocalEvents.ClearLightningInvoice)
                scope.launch {
                    delay(100)
                    focusRequester.requestFocus()
                }
            }
        )
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        withImePadding = true,
        onProgressStyle = OnProgressStyle.Disabled,
        sideEffectsHandler = {
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

                    AnimatedVisibility(visible = showLedgerAssetWarning) {
                        GreenCard(
                            padding = 8, colors = CardDefaults.elevatedCardColors(
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

                    val accountTypeSubtitle = accountAsset?.account?.let { account ->
                        when {
                            account.isLightning -> null
                            account.isAmp -> stringResource(Res.string.id_account_type_amp)
                            account.isMultisig -> stringResource(Res.string.id_account_type_2fa_protected)
                            account.isLiquid -> stringResource(Res.string.id_account_type_standard)
                            else -> null
                        }
                    }

                    GreenAsset(
                        assetBalance = AssetBalance.create(asset),
                        session = viewModel.sessionOrNull,
                        title = stringResource(Res.string.id_asset),
                        subtitle = accountTypeSubtitle,
                        trailingContent = if (showSwap) {
                            { LightningReadyBadge() }
                        } else null
                    )
                }

                if (accountAsset == null && !viewModel.session.isWatchOnlyValue) {
                    GreenButton(text = stringResource(Res.string.id_create_new_account), modifier = Modifier.fillMaxWidth()) {
                        viewModel.postEvent(ReceiveViewModel.LocalEvents.CreateAccount)
                    }
                }

                AnimatedVisibility(showSwap) {

                    Column {
                        Text(
                            text = stringResource(Res.string.id_payer_sends),
                            style = bodyLarge,
                            color = whiteMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )

                        val options = listOf("Liquid", "Lightning")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = {
                                        viewModel.isReverseSubmarineSwap.value = index == 1
                                    },
                                    selected = (index == 1) == isReverseSubmarineSwap
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = (accountAsset?.account?.isLightning == true && !showLightningOnChainAddress) || showRequestAmount || (isReverseSubmarineSwap && receiveAddress == null)) {

                    GreenColumn(padding = 0, space = 8) {

                        GreenAmountField(
                            value = amount,
                            onValueChange = viewModel.amount.onValueChange(),
                            secondaryValue = receiveAmountData.exchange,
                            assetId = accountAsset?.assetId,
                            session = viewModel.sessionOrNull,
                            title = if (showRequestAmount) stringResource(
                                Res.string.id_request_amount
                            ) else stringResource(Res.string.id_amount),
                            helperText = receiveAmountData.error,
                            enabled = !onProgress,
                            denomination = denomination,
                            focusRequester = focusRequester,
                            isReadyOnly = accountAsset?.account?.isLightning == true && receiveAddress != null,
                            onEditClick = {
                                viewModel.postEvent(ReceiveViewModel.LocalEvents.ClearLightningInvoice)
                            },
                            onDenominationClick = {
                                viewModel.postEvent(Events.SelectDenomination)
                            }
                        )

                        AnimatedNullableVisibility(receiveAmountData.liquidityFee) {
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

                if (!isLightningOrSwap && (receiveAddress.isNotBlank() || accountAsset?.account?.isLightning == false)) {

                    Column {

                        Text(
                            stringResource(Res.string.id_account_address),
                            style = labelMedium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )

                        Card {

                            Box(modifier = Modifier.padding(bottom = 8.dp)) {

                                if (accountAsset?.account?.isLightning == false) {
                                    IconButton(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 2.dp, end = 2.dp)
                                            .appTestTag("refresh"),
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
                                            maxLines = 6,
                                            onCopyClick = {
                                                viewModel.postEvent(ReceiveViewModel.LocalEvents.CopyAddress)
                                            }
                                        )

                                        val showVerifyOnDevice by viewModel.showVerifyOnDevice.collectAsStateWithLifecycle()
                                        AnimatedVisibility(visible = showVerifyOnDevice) {
                                            Column {
                                                GreenButton(
                                                    text = stringResource(Res.string.id_verify_on_device),
                                                    type = GreenButtonType.OUTLINE,
                                                    color = GreenButtonColor.GREENER,
                                                    icon = PhosphorIcons.Regular.SealCheck,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = !onProgress
                                                ) {
                                                    viewModel.postEvent(ReceiveViewModel.LocalEvents.VerifyOnDevice)
                                                }

                                                Text(
                                                    text = stringResource(Res.string.id_please_verify_that_the_address),
                                                    textAlign = TextAlign.Center,
                                                    color = whiteMedium,
                                                    style = bodyMedium,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                AnimatedVisibility(visible = !isLightningOrSwap && receiveAddress.isNotBlank()) {
                    GreenButton(
                        text = stringResource(Res.string.id_share),
                        icon = PhosphorIcons.Regular.ShareNetwork,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !onProgress,
                        testTag = "share_button"
                    ) {
                        scope.launch {
                            viewModel.postEvent(
                                NavigateDestinations.Menu(
                                    title = getString(Res.string.id_share),
                                    entries = MenuEntryList(
                                        listOf(
                                            MenuEntry(
                                                title = getString(Res.string.id_address),
                                                iconRes = "text-aa"
                                            ),
                                            MenuEntry(
                                                title = getString(Res.string.id_qr_code),
                                                iconRes = "qr-code"
                                            ),
                                        )
                                    )
                                )
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = (isReverseSubmarineSwap || accountAsset?.account?.isLightning == true) && !showLightningOnChainAddress && receiveAddress.isNullOrBlank()) {

                    Column {
                        if (isReverseSubmarineSwap) {
                            Text(
                                text = stringResource(Res.string.id_you_will_receive_liquid_bitcoin),
                                style = bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        GreenButton(
                            text = stringResource(Res.string.id_confirm),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = buttonEnabled
                        ) {
                            viewModel.postEvent(ReceiveViewModel.LocalEvents.CreateInvoice)
                        }
                    }
                }

//                AnimatedVisibility(visible = accountAsset?.account?.isLightning == true) {
//                    GreenButton(
//                        text = stringResource(if (showLightningOnChainAddress) Res.string.id_show_lightning_invoice else Res.string.id_show_onchain_address),
//                        modifier = Modifier.fillMaxWidth(),
//                        type = GreenButtonType.TEXT,
//                        color = GreenButtonColor.WHITE,
//                        icon = PhosphorIcons.Regular.Info,
//                        size = GreenButtonSize.SMALL,
//                        enabled = !onProgress
//                    ) {
//                        viewModel.postEvent(ReceiveViewModel.LocalEvents.ToggleLightning)
//                    }
//                }
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
