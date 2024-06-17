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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_check_our_6_easy_steps_to_be
import blockstream_green.common.generated.resources.id_increase_the_security_of_your
import blockstream_green.common.generated.resources.id_latest_transactions
import blockstream_green.common.generated.resources.id_learn_more_about_amp_the_assets
import blockstream_green.common.generated.resources.id_your_transactions_will_be_shown
import blockstream_green.common.generated.resources.shield_warning
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.overview.AccountOverviewViewModel
import com.blockstream.common.models.overview.AccountOverviewViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.components.BottomNav
import com.blockstream.compose.components.GreenAccountCard
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenContentCard
import com.blockstream.compose.components.GreenGradient
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.dialogs.LightningShortcutDialog
import com.blockstream.compose.sheets.CameraBottomSheet
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
    MainMenuBottomSheet.getResult {
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

            MainMenuEntry.REDEPOSIT -> {
                viewModel.postEvent(NavigateDestinations.Redeposit(accountAsset = viewModel.accountAsset.value!!, isRedeposit2FA = false))
            }
        }
    }

    CameraBottomSheet.getResult {
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
                    onWarningClick = if (accountBalance.hasNoTwoFactor || accountBalance.hasExpiredUtxos) {
                        {
                            if(accountBalance.hasExpiredUtxos){
                                viewModel.postEvent(NavigateDestinations.ReEnable2FA)
                            }else{
                                viewModel.postEvent(NavigateDestinations.EnableTwoFactor(accountBalance.account.network))
                            }
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

            if (accountBalance.hasNoTwoFactor) {
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
                                painter = painterResource(Res.drawable.shield_warning),
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(Res.string.id_increase_the_security_of_your),
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
                            .padding(top = 8.dp, bottom = 8.dp),
                        title = stringResource(Res.string.id_learn_more_about_amp_the_assets),
                        message = stringResource(
                            Res.string.id_check_our_6_easy_steps_to_be
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
                items(items = it, key = {
                    it.assetId
                }) {
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
                            .padding(all = 16.dp)
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

            transactions.data()?.let {
                items(items = it, key = { it.transaction.txHash.hashCode() + it.transaction.txType.gdkType.hashCode() }) {
                    GreenTransaction(transactionLook = it) {
                        viewModel.postEvent(Events.Transaction(transaction = it.transaction))
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
                isWatchOnly = viewModel.sessionOrNull?.isNoBlobWatchOnly == true,
                isSweepEnabled = viewModel.sessionOrNull?.defaultNetworkOrNull?.isBitcoin == true,
                onSendClick = {
                    viewModel.postEvent(AccountOverviewViewModel.LocalEvents.Send)
                }, onReceiveClick = {
                    viewModel.postEvent(AccountOverviewViewModel.LocalEvents.Receive)
                }, onCircleClick = {
                    // bottomSheetNavigator.show(MainMenuBottomSheet)
                    viewModel.postEvent(
                        NavigateDestinations.Camera(
                            isDecodeContinuous = true,
                            parentScreenName = viewModel.screenName()
                        )
                    )
                }
            )
        }
    }
}
