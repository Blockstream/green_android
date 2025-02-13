package com.blockstream.compose.screens.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.box_arrow_down
import blockstream_green.common.generated.resources.id_archive_account
import blockstream_green.common.generated.resources.id_create_account
import blockstream_green.common.generated.resources.id_create_your_first_account_to
import blockstream_green.common.generated.resources.id_d_assets_in_total
import blockstream_green.common.generated.resources.id_latest_transactions
import blockstream_green.common.generated.resources.id_remove
import blockstream_green.common.generated.resources.id_rename_account
import blockstream_green.common.generated.resources.id_welcome_to_your_wallet
import blockstream_green.common.generated.resources.id_your_transactions_will_be_shown
import blockstream_green.common.generated.resources.text_aa
import blockstream_green.common.generated.resources.trash
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountBalance
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModelAbstract
import com.blockstream.common.models.settings.DenominationExchangeRateViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.BottomNav
import com.blockstream.compose.components.GreenAccountCard
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.components.PopupMenu
import com.blockstream.compose.components.PopupState
import com.blockstream.compose.components.Promo
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.components.WalletBalance
import com.blockstream.compose.dialogs.AppRateDialog
import com.blockstream.compose.dialogs.ArchivedAccountsDialog
import com.blockstream.compose.dialogs.DenominationExchangeDialog
import com.blockstream.compose.dialogs.WalletOverviewMenuDialog
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.toMenuDpOffset
import com.blockstream.compose.managers.askForNotificationPermissions
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.sheets.MainMenuEntry
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_background
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.views.LightningInfo
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenGradient
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.components.GreenSpacer
import com.blockstream.ui.navigation.getResult
import com.blockstream.ui.utils.bottom
import com.blockstream.ui.utils.plus
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource


@Composable
fun WalletOverviewScreen(
    viewModel: WalletOverviewViewModelAbstract
) {

    var denominationExchangeRateViewModel by remember {
        mutableStateOf<DenominationExchangeRateViewModel?>(null)
    }
    var appRateViewModel by remember {
        mutableStateOf<SimpleGreenViewModel?>(null)
    }
    var archivedAccountsViewModel by remember {
        mutableStateOf<ArchivedAccountsViewModel?>(null)
    }
    var overviewMenuViewModel by remember {
        mutableStateOf<WalletOverviewViewModelAbstract?>(null)
    }

    askForNotificationPermissions(viewModel)

    NavigateDestinations.MainMenu.getResult<MainMenuEntry> {
        when (it) {
            MainMenuEntry.SCAN -> {
                viewModel.postEvent(
                    NavigateDestinations.Camera(
                        isDecodeContinuous = true,
                        parentScreenName = viewModel.screenName()
                    )
                )
            }

            MainMenuEntry.ACCOUNT_TRANSFER -> {
                viewModel.postEvent(NavigateDestinations.AccountExchange(greenWallet = viewModel.greenWallet))
            }

            MainMenuEntry.REDEPOSIT -> {
                viewModel.postEvent(NavigateDestinations.Redeposit(greenWallet = viewModel.greenWallet, accountAsset = viewModel.session.activeAccount.value!!.accountAsset, isRedeposit2FA = false))
            }

            MainMenuEntry.BUY_SELL -> {
                viewModel.postEvent(NavigateDestinations.OnOffRamps(greenWallet = viewModel.greenWallet))
            }
        }
    }

    NavigateDestinations.Camera.getResult<ScanResult> {
        viewModel.postEvent(Events.HandleUserInput(it.result, isQr = true))
    }

    overviewMenuViewModel?.also {
        WalletOverviewMenuDialog(viewModel = viewModel) {
            overviewMenuViewModel = null
        }
    }

    denominationExchangeRateViewModel?.also {
        DenominationExchangeDialog(viewModel = it) {
            denominationExchangeRateViewModel = null
        }
    }

    appRateViewModel?.also {
        AppRateDialog(viewModel = it) {
            appRateViewModel = null
        }
    }

    archivedAccountsViewModel?.also {
        ArchivedAccountsDialog(viewModel = it) {
            archivedAccountsViewModel = null
        }
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }
    val state = rememberPullToRefreshState()
    if (isRefreshing) {
        LaunchedEffect(true) {
            viewModel.postEvent(WalletOverviewViewModel.LocalEvents.Refresh)
            delay(1500)
            isRefreshing = false
        }
    }

    SetupScreen(viewModel = viewModel, sideEffectsHandler = {
        when (it) {
            is SideEffects.OpenDenominationExchangeRate -> {
                denominationExchangeRateViewModel =
                    DenominationExchangeRateViewModel(viewModel.greenWallet)
            }

            is SideEffects.AppReview -> {
                appRateViewModel = SimpleGreenViewModel(viewModel.greenWallet)
            }

            is WalletOverviewViewModel.LocalSideEffects.AccountArchivedDialog -> {
                archivedAccountsViewModel =
                    ArchivedAccountsViewModel(viewModel.greenWallet)
            }

            is SideEffects.OpenDialog -> {
                overviewMenuViewModel = viewModel
            }
        }
    }, withPadding = false, withBottomInsets = false) {

        Box(
            Modifier
                .fillMaxSize()
        ) {

            val isWalletOnboarding by viewModel.isWalletOnboarding.collectAsStateWithLifecycle()
            val accounts by viewModel.accounts.collectAsStateWithLifecycle()
            val alerts by viewModel.alerts.collectAsStateWithLifecycle()
            val lightningInfo by viewModel.lightningInfo.collectAsStateWithLifecycle()
            val transactions by viewModel.transactions.collectAsStateWithLifecycle()
            val innerPadding = LocalInnerPadding.current
            val density = LocalDensity.current

            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                contentPadding = innerPadding.bottom().plus(PaddingValues(bottom = if(viewModel.appInfo.enableNewFeatures) (80.dp + 16.dp) else (96.dp))),
                modifier = Modifier.pullToRefresh(isRefreshing = isRefreshing, state = state) {
                    isRefreshing = true
                }) {
                if (!viewModel.isLightningShortcut) {
                    item(key = "WalletBalance") {
                        WalletBalance(viewModel)
                    }
                }

                item(key = "WalletAssets") {
                    WalletAssets(viewModel = viewModel) {
                        viewModel.postEvent(NavigateDestinations.WalletAssets(greenWallet = viewModel.greenWallet))
                    }
                }

                item(key = "GreenSpacer") {
                    GreenSpacer()
                }

                if (!isWalletOnboarding || viewModel.appInfo.enableNewFeatures) {

                    items(items = alerts, key = {
                        it.hashCode()
                    }) {
                        GreenAlert(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 6.dp), alertType = it, viewModel = viewModel
                        )
                    }

                    items(items = accounts, key = {
                        it.account.id
                    }) {
                        val popupState = remember {
                            PopupState()
                        }
                        val expandedAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
                        var cardSize by remember {
                            mutableStateOf(IntSize.Zero)
                        }

                        val menuEntries = when {
                            viewModel.greenWallet.isWatchOnly || viewModel.isLightningShortcut -> emptyList()
                            it.account.isLightning -> listOf(
                                MenuEntry(
                                    title = stringResource(Res.string.id_remove),
                                    iconRes = Res.drawable.trash,
                                    onClick = {
                                        viewModel.postEvent(Events.RemoveAccount(it.account))
                                    }
                                )
                            )

                            else -> listOfNotNull(
                                MenuEntry(
                                    title = stringResource(Res.string.id_rename_account),
                                    iconRes = Res.drawable.text_aa,
                                    onClick = {
                                        viewModel.postEvent(
                                            NavigateDestinations.RenameAccount(
                                                greenWallet = viewModel.greenWallet,
                                                account = it.account
                                            )
                                        )
                                    }
                                ),
                                if (viewModel.accounts.value.size > 1) {
                                    MenuEntry(
                                        title = stringResource(Res.string.id_archive_account),
                                        iconRes = Res.drawable.box_arrow_down,
                                        onClick = {
                                            viewModel.postEvent(Events.ArchiveAccount(it.account))
                                        }
                                    )
                                } else null
                            )
                        }

                        Box {
                            GreenAccountCard(
                                modifier = Modifier
                                    .padding(bottom = 1.dp)
                                    .onSizeChanged {
                                        cardSize = it
                                    },
                                account = it,
                                isExpanded = it.account.id == expandedAccount?.id,
                                session = viewModel.sessionOrNull,
                                onArrowClick = if (!viewModel.isLightningShortcut) {
                                    {
                                        viewModel.postEvent(
                                            NavigateDestinations.AccountOverview(
                                                greenWallet = viewModel.greenWallet,
                                                accountAsset = it.accountAsset
                                            )
                                        )
                                    }
                                } else null,
                                onWarningClick = if (it.hasNoTwoFactor || it.hasExpiredUtxos || it.hasTwoFactorReset) {
                                    {
                                        if (it.hasTwoFactorReset) {
                                            viewModel.postEvent(
                                                NavigateDestinations.TwoFactorReset(
                                                    greenWallet = viewModel.greenWallet,
                                                    network = it.account.network,
                                                    twoFactorReset = viewModel.sessionOrNull?.twoFactorReset(
                                                        it.account.network
                                                    )?.value
                                                )
                                            )
                                        } else if (it.hasExpiredUtxos) {
                                            viewModel.postEvent(
                                                NavigateDestinations.ReEnable2FA(
                                                    greenWallet = viewModel.greenWallet
                                                )
                                            )
                                        } else if (it.hasNoTwoFactor) {
                                            viewModel.postEvent(
                                                NavigateDestinations.EnableTwoFactor(
                                                    greenWallet = viewModel.greenWallet,
                                                    network = it.account.network
                                                )
                                            )
                                        }
                                    }
                                } else null,
                                onClick = {
                                    viewModel.postEvent(
                                        Events.SetAccountAsset(
                                            accountAsset = it.account.accountAsset,
                                            setAsActive = true
                                        )
                                    )
                                }, onLongClick = { _: AccountBalance, offset: Offset ->
                                    if (menuEntries.isNotEmpty()) {
                                        popupState.offset.value =
                                            offset.toMenuDpOffset(cardSize, density)
                                        popupState.isContextMenuVisible.value = true
                                    }
                                }
                            )

                            if (menuEntries.isNotEmpty()) {
                                PopupMenu(
                                    state = popupState,
                                    entries = menuEntries
                                )
                            }
                        }
                    }

                    lightningInfo?.also { lightningInfo ->
                        item(key = "LightningInfo") {
                            LightningInfo(lightningInfoLook = lightningInfo, onLearnMore = {
                                viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ClickLightningLearnMore)
                            })
                        }
                    }

                    item(key = "Promo") {
                        Promo(
                            viewModel = viewModel,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
                        )
                    }

                    item(key = "Transactions Header") {
                        Text(
                            text = stringResource(Res.string.id_latest_transactions),
                            style = titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp)
                                .padding(bottom = 8.dp)
                        )

                        if (transactions.isLoading()) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .padding(all = 32.dp)
                                    .height(1.dp)
                                    .fillMaxWidth()
                            )
                        } else if (transactions.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.id_your_transactions_will_be_shown),
                                style = bodyMedium,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }

                    items(items = transactions.data() ?: listOf(), key = { tx ->
                        tx.transaction.account.id.hashCode() + tx.transaction.txHash.hashCode() + tx.transaction.txType.gdkType.hashCode()
                    }) { item ->
                        GreenTransaction(transactionLook = item) {
                            viewModel.postEvent(Events.Transaction(transaction = it.transaction))
                        }
                    }
                }
            }

            if(!viewModel.appInfo.enableNewFeatures){
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    GreenGradient(
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                        size = 48
                    )

                    BottomNav(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp)
                            .padding(innerPadding.bottom())
                            .padding(bottom = 8.dp)
                        ,
                        canSend = viewModel.sessionOrNull?.canSendTransaction ?: false,
                        isSweepEnabled = viewModel.sessionOrNull?.defaultNetworkOrNull?.isBitcoin == true,
                        showMenu = true,
                        onSendClick = {
                            viewModel.postEvent(WalletOverviewViewModel.LocalEvents.Send)
                        }, onReceiveClick = {
                            viewModel.postEvent(WalletOverviewViewModel.LocalEvents.Receive)
                        }, onCircleClick = {
                            viewModel.postEvent(NavigateDestinations.MainMenu(isTestnet = viewModel.greenWallet.isTestnet))
                        }
                    )
                }
            }

            if (isWalletOnboarding && !viewModel.appInfo.enableNewFeatures) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .noRippleClickable {
                            // catch all clicks
                        }
                        .background(md_theme_background.copy(alpha = 0.9f))
                ) {

                    GreenColumn(
                        padding = 32,
                        space = 16,
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        if (!LocalInspectionMode.current) {
                            Rive(RiveAnimation.WALLET)
                        }

                        Text(
                            text = stringResource(Res.string.id_welcome_to_your_wallet),
                            style = titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Text(
                            text = stringResource(Res.string.id_create_your_first_account_to),
                            style = bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        GreenButton(text = stringResource(Res.string.id_create_account)) {
                            viewModel.postEvent(Events.ChooseAccountType(isFirstAccount = true))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletAssets(viewModel: WalletOverviewViewModelAbstract, onClick: () -> Unit = {}) {
    val totalAssets by viewModel.totalAssets.collectAsStateWithLifecycle()
    val assetIcons by viewModel.assetIcons.collectAsStateWithLifecycle()
    val assetsVisibility by viewModel.assetsVisibility.collectAsStateWithLifecycle()

    AnimatedVisibility(visible = assetsVisibility != false) {
        GreenRow(
            padding = 0,
            space = 8,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
        ) {
            Box(modifier = Modifier
                .height(24.dp)
                .clickable {
                    onClick.invoke()
                }) {
                assetIcons.forEachIndexed { index, asset ->
                    val size = 24
                    val padding = (size / (1.5 + (0.1 * index)) * index)

                    Image(
                        painter = asset.assetIcon(session = viewModel.sessionOrNull),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = padding.dp)
                            .size(size.dp)
                            .clip(CircleShape)
                            .align(Alignment.BottomEnd)
                            .border(1.dp, Color.Black, CircleShape)
                            .zIndex(-index.toFloat())
                    )
                }
            }

            AnimatedVisibility(visible = assetsVisibility == true) {
                Text(
                    text = stringResource(Res.string.id_d_assets_in_total, totalAssets),
                    style = labelLarge.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    color = green,
                    modifier = Modifier.clickable {
                        onClick.invoke()
                    }
                )
            }
        }
    }
}