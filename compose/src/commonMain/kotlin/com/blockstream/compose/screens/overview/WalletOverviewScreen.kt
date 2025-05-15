package com.blockstream.compose.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_all
import blockstream_green.common.generated.resources.id_assets
import blockstream_green.common.generated.resources.id_bitcoin_price
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_latest_transactions
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_lightning
import blockstream_green.common.generated.resources.id_transfer_your_funds
import blockstream_green.common.generated.resources.id_transfer_your_funds_from_your_old_wallet
import blockstream_green.common.generated.resources.id_welcome_to_blockstream
import blockstream_green.common.generated.resources.id_you_dont_have_any_assets_yet
import blockstream_green.common.generated.resources.id_your_wallet_has_been_created
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.archived.ArchivedAccountsViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModelAbstract
import com.blockstream.common.models.settings.DenominationExchangeRateViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.ListHeader
import com.blockstream.compose.components.Promo
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.components.WalletBalance
import com.blockstream.compose.dialogs.AppRateDialog
import com.blockstream.compose.dialogs.ArchivedAccountsDialog
import com.blockstream.compose.dialogs.DenominationExchangeDialog
import com.blockstream.compose.dialogs.WalletOverviewMenuDialog
import com.blockstream.compose.extensions.itemsSpaced
import com.blockstream.compose.managers.askForNotificationPermissions
import com.blockstream.compose.screens.overview.components.BitcoinPriceChart
import com.blockstream.compose.sheets.MainMenuEntry
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_background
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.views.LightningInfo
import com.blockstream.ui.common.OnScreenFocus
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenSpacer
import com.blockstream.ui.navigation.LocalInnerPadding
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
                        isDecodeContinuous = true, parentScreenName = viewModel.screenName()
                    )
                )
            }

            MainMenuEntry.ACCOUNT_TRANSFER -> {
                viewModel.postEvent(NavigateDestinations.AccountExchange(greenWallet = viewModel.greenWallet))
            }

            MainMenuEntry.REDEPOSIT -> {
                viewModel.postEvent(
                    NavigateDestinations.Redeposit(
                        greenWallet = viewModel.greenWallet,
                        accountAsset = viewModel.session.activeAccount.value!!.accountAsset,
                        isRedeposit2FA = false
                    )
                )
            }

            MainMenuEntry.BUY_SELL -> {
                viewModel.postEvent(NavigateDestinations.OnOffRamps(greenWallet = viewModel.greenWallet))
            }
        }
    }

    NavigateDestinations.Camera.getResult<ScanResult> {
        viewModel.postEvent(Events.HandleUserInput(it.result, isQr = true))
    }

    NavigateDestinations.Accounts.getResult<AccountAssetBalance> {
        viewModel.postEvent(
            NavigateDestinations.AccountOverview(
                greenWallet = viewModel.greenWallet,
                accountAsset = it.accountAsset
            )
        )
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

    OnScreenFocus(viewModel::refetchBitcoinPriceHistory)

    SetupScreen(viewModel = viewModel, sideEffectsHandler = {
        when (it) {
            is SideEffects.OpenDenominationExchangeRate -> {
                denominationExchangeRateViewModel = DenominationExchangeRateViewModel(viewModel.greenWallet)
            }

            is SideEffects.AppReview -> {
                appRateViewModel = SimpleGreenViewModel(viewModel.greenWallet)
            }

            is WalletOverviewViewModel.LocalSideEffects.AccountArchivedDialog -> {
                archivedAccountsViewModel = ArchivedAccountsViewModel(viewModel.greenWallet)
            }

            is SideEffects.OpenDialog -> {
                overviewMenuViewModel = viewModel
            }
        }
    }, withPadding = false, withBottomInsets = false) {

        Box(modifier = Modifier.fillMaxSize()) {

            val isWalletOnboarding by viewModel.showWalletOnboarding.collectAsStateWithLifecycle()
            val alerts by viewModel.alerts.collectAsStateWithLifecycle()
            val assets by viewModel.assets.collectAsStateWithLifecycle()
            val showHardwareTransferFunds by viewModel.showHardwareTransferFunds.collectAsStateWithLifecycle()
            val transaction by viewModel.transaction.collectAsStateWithLifecycle()
            val lightningInfo by viewModel.lightningInfo.collectAsStateWithLifecycle()
            val innerPadding = LocalInnerPadding.current

            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                contentPadding = innerPadding
                    .bottom()
                    .plus(PaddingValues(horizontal = 16.dp))
                    .plus(PaddingValues(bottom = (80.dp + 16.dp))),
                modifier = Modifier.pullToRefresh(isRefreshing = isRefreshing, state = state) {
                    isRefreshing = true
                }
            ) {
                item(key = "WalletBalance") {
                    WalletBalance(viewModel = viewModel)
                }

                if (!isWalletOnboarding) {

                    if (alerts.isNotEmpty()) {
                        item(key = "AlertsHeader") {
                            GreenSpacer(16)
                        }
                    }

                    items(items = alerts, key = {
                        it.hashCode()
                    }) {
                        GreenAlert(
                            modifier = Modifier
                                .padding(bottom = 6.dp), alertType = it, viewModel = viewModel
                        )
                    }

                    item(key = "Promo") {
                        Promo(
                            viewModel = viewModel,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
                        )
                    }


                    item(key = "AssetsHeader") {
                        ListHeader(title = stringResource(Res.string.id_assets))
                    }

                    if (showHardwareTransferFunds) {
                        item(key = "HardwareTransferFunds") {
                            GreenAlert(
                                title = stringResource(Res.string.id_transfer_your_funds),
                                message = stringResource(Res.string.id_transfer_your_funds_from_your_old_wallet),
                                isBlue = true,
                                primaryButton = stringResource(Res.string.id_learn_more),
                                onPrimaryClick = {

                                }
                            )
                        }
                    } else {
                        if (assets.isLoading()) {
                            item(key = "AssetsLoading") {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .height(1.dp)
                                        .padding(horizontal = 32.dp)
                                        .fillMaxWidth()
                                )
                            }
                        } else if (assets.isNotEmpty()) {
                            itemsSpaced(assets.data() ?: emptyList()) { asset ->
                                GreenAsset(
                                    assetBalance = asset,
                                    session = viewModel.sessionOrNull
                                ) {
                                    viewModel.navigateToAccountOverview(asset.asset)
                                }
                            }
                        } else {
                            item(key = "AssetsEmpty") {
                                Text(
                                    text = stringResource(Res.string.id_you_dont_have_any_assets_yet),
                                    style = bodyMedium,
                                    textAlign = TextAlign.Center,
                                    fontStyle = FontStyle.Italic,
                                    color = whiteMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }

                    item(key = "BitcoinPrice") {
                        ListHeader(title = stringResource(Res.string.id_bitcoin_price))
                    }

                    item {
                        BitcoinPriceChart(viewModel.bitcoinChartData, {
                            viewModel.navigateToBuy()
                        })
                    }

                    lightningInfo?.also { lightningInfo ->
                        item(key = "LightningHeader") {
                            ListHeader(title = stringResource(Res.string.id_lightning))
                        }

                        item(key = "LightningInfo") {
                            LightningInfo(lightningInfoLook = lightningInfo, onSweepClick = {
                                viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ClickLightningSweep)
                            }, onLearnMore = {
                                viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ClickLightningLearnMore)
                            })
                        }
                    }

                    transaction.data()?.also { transaction ->
                        item(key = "LatestTransactionsHeader") {
                            ListHeader(
                                title = stringResource(Res.string.id_latest_transactions),
                                cta = stringResource(Res.string.id_all),
                                onClick = {
                                    viewModel.postEvent(NavigateDestinations.Transact(greenWallet = viewModel.greenWallet))
                                }
                            )
                        }

                        item(key = "LatestTransactions") {
                            GreenTransaction(transactionLook = transaction) {
                                viewModel.postEvent(Events.Transaction(transaction = it.transaction))
                            }
                        }
                    }
                }
            }

            if (isWalletOnboarding) {
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
                        space = 0,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Rive(RiveAnimation.WALLET)

                        GreenColumn(
                            padding = 0,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = stringResource(Res.string.id_welcome_to_blockstream),
                                style = titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Text(
                                text = stringResource(Res.string.id_your_wallet_has_been_created),
                                style = bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            GreenButton(text = stringResource(Res.string.id_continue)) {
                                viewModel.showWalletOnboarding.value = false
                            }
                        }
                    }
                }
            }
        }
    }
}