package com.blockstream.compose.screens.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.overview.AccountOverviewViewModel
import com.blockstream.common.models.overview.AccountOverviewViewModelAbstract
import com.blockstream.common.models.overview.AccountOverviewViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAccountCard
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenContentCard
import com.blockstream.compose.components.GreenGradient
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.dialogs.LightningShortcutDialog
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.MainMenuBottomSheet
import com.blockstream.compose.sheets.MainMenuEntry
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.reachedBottom
import com.blockstream.compose.views.LightningInfo
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import org.koin.core.parameter.parametersOf


@Parcelize
data class AccountOverviewScreen(
    val greenWallet: GreenWallet,
    val accountAsset: AccountAsset
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AccountOverviewViewModel>() {
            parametersOf(greenWallet, accountAsset)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        AccountOverviewScreen(viewModel = viewModel)
    }

    @IgnoredOnParcel
    override val key = uniqueScreenKey
}

@Composable
fun AccountOverviewScreen(
    viewModel: AccountOverviewViewModelAbstract
) {
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current

    getNavigationResult<MainMenuEntry>(MainMenuBottomSheet.resultKey).value?.also {
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
                viewModel.postEvent(NavigateDestinations.AccountExchange)
            }
        }
    }

    getNavigationResult<ScanResult>(CameraBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(Events.HandleUserInput(it.result, isQr = true))
    }

    var lightningShortcutViewModel by remember {
        mutableStateOf<GreenViewModel?>(null)
    }

    lightningShortcutViewModel?.also {
        LightningShortcutDialog(viewModel = it) {
            lightningShortcutViewModel = null
        }
    }

    HandleSideEffect(viewModel = viewModel) {
        if (it is SideEffects.LightningShortcut) {
            lightningShortcutViewModel = SimpleGreenViewModel(viewModel.greenWallet)
        }
    }

    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.postEvent(AccountOverviewViewModel.LocalEvents.Refresh)
            delay(1500)
            state.endRefresh()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(state.nestedScrollConnection)
    ) {

        val accountBalance by viewModel.accountBalance.collectAsStateWithLifecycle()
        val showAmpInfo by viewModel.showAmpInfo.collectAsStateWithLifecycle()
        val alerts by viewModel.alerts.collectAsStateWithLifecycle()
        val assets by viewModel.assets.collectAsStateWithLifecycle()
        val lightningInfo by viewModel.lightningInfo.collectAsStateWithLifecycle()
        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
        val hasMoreTransactions by viewModel.hasMoreTransactions.collectAsStateWithLifecycle()

        val listState: LazyListState = rememberLazyListState()
        val reachedBottom: Boolean by remember { derivedStateOf { listState.reachedBottom() } }

        LaunchedEffect(reachedBottom) {
            if (reachedBottom && hasMoreTransactions && transactions.isSuccess()) {
                viewModel.postEvent(AccountOverviewViewModel.LocalEvents.LoadMoreTransactions)
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), state = listState) {

            items(alerts) {
                GreenAlert(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp), alertType = it, viewModel = viewModel
                )
            }

            item {
                GreenAccountCard(
                    modifier = Modifier
                        .padding(bottom = 1.dp),
                    account = accountBalance,
                    isExpanded = true,
                    session = viewModel.sessionOrNull,
                    onCopyClick = if (accountBalance.account.isAmp) {
                        {
                            viewModel.postEvent(AccountOverviewViewModel.LocalEvents.CopyAccountId)
                        }
                    } else null,
                    onWarningClick = if (accountBalance.warningTwoFactor) {
                        {
                            viewModel.postEvent(NavigateDestinations.EnableTwoFactor)
                        }
                    } else null,
                    onClick = {
                        viewModel.postEvent(
                            Events.SetAccountAsset(
                                accountAsset = accountBalance.accountAsset,
                                setAsActive = true
                            )
                        )
                    }
                )
            }

            if (accountBalance.warningTwoFactor) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = green20,
                            contentColor = green
                        ),
                        onClick = {
                            viewModel.postEvent(
                                NavigateDestinations.TwoFactorAuthentication(
                                    network = accountBalance.account.network
                                )
                            )
                        },
                    ) {
                        GreenRow(padding = 8, space = 8) {
                            Icon(
                                painter = painterResource(id = R.drawable.shield_warning),
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.id_increase_the_security_of_your),
                                style = labelMedium
                            )
                        }
                    }
                }
            }

            if (showAmpInfo) {
                item {
                    GreenContentCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        title = stringResource(R.string.id_learn_more_about_amp_the_assets),
                        message = stringResource(
                            R.string.id_check_our_6_easy_steps_to_be
                        ),
                        onClick = {
                            viewModel.postEvent(Events.OpenBrowser(Urls.HELP_AMP_ASSETS))
                        }
                    )
                }
            }

            lightningInfo?.also { lightningInfo ->
                item {
                    LightningInfo(lightningInfoLook = lightningInfo, onSweepClick = {
                        viewModel.postEvent(AccountOverviewViewModel.LocalEvents.ClickLightningSweep)
                    }, onLearnMore = {
                        viewModel.postEvent(AccountOverviewViewModel.LocalEvents.ClickLightningLearnMore)
                    })
                }
            }

            item {
                if (assets.isLoading()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .padding(all = 16.dp)
                            .height(1.dp)
                            .fillMaxWidth()
                    )
                }
            }

            assets.data()?.also {
                items(it) {
                    GreenAsset(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 2.dp),
                        assetBalance = it,
                        session = viewModel.sessionOrNull
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.AssetDetails(
                                assetId = it.assetId,
                                accountAsset = viewModel.accountAsset.value
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.id_latest_transactions),
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
                            .padding(all = 16.dp)
                            .height(1.dp)
                            .fillMaxWidth()
                    )
                } else if (transactions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.id_your_transactions_will_be_shown),
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

            transactions.data()?.let {
                itemsIndexed(it) { index, item ->
                    GreenTransaction(transactionLook = item) {
                        viewModel.postEvent(Events.Transaction(transaction = item.transaction))
                    }
                }
            }
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            GreenGradient(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                size = 76
            )

            BottomNav(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                isWatchOnly = viewModel.greenWallet.isWatchOnly,
                isSweepEnabled = viewModel.sessionOrNull?.defaultNetworkOrNull?.isBitcoin == true,
                onSendClick = {
                    viewModel.postEvent(AccountOverviewViewModel.LocalEvents.Send)
                }, onReceiveClick = {
                    viewModel.postEvent(AccountOverviewViewModel.LocalEvents.Receive)
                }, onCircleClick = {
                    bottomSheetNavigator.show(MainMenuBottomSheet)
                }
            )
        }
    }
}


@Composable
@Preview
fun AccountOverviewPreview() {
    GreenPreview {
        AccountOverviewScreen(viewModel = AccountOverviewViewModelPreview.create())
    }
}
