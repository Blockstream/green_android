package com.blockstream.compose.screens.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.blockstream.common.Urls
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.models.overview.AccountOverviewViewModel
import com.blockstream.common.models.overview.AccountOverviewViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAccountCard
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenContentCard
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.sheets.MainMenuEntry
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.views.LightningInfo
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.utils.bottom
import com.blockstream.compose.utils.reachedBottom
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountOverviewScreen(
    viewModel: AccountOverviewViewModelAbstract
) {
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
                viewModel.postEvent(
                    NavigateDestinations.Redeposit(
                        greenWallet = viewModel.greenWallet,
                        accountAsset = viewModel.accountAsset.value!!,
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

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    val state = rememberPullToRefreshState()
    if (isRefreshing) {
        LaunchedEffect(true) {
            viewModel.postEvent(AccountOverviewViewModel.LocalEvents.Refresh)
            delay(1500)
            isRefreshing = false
        }
    }

    SetupScreen(viewModel = viewModel, withPadding = false, withBottomInsets = false) {
        Box(
            Modifier
                .fillMaxSize()
                .pullToRefresh(isRefreshing = isRefreshing, state = state) {
                    isRefreshing = true
                }
        ) {

            val accountBalance by viewModel.accountBalance.collectAsStateWithLifecycle()
            val showAmpInfo by viewModel.showAmpInfo.collectAsStateWithLifecycle()
            val alerts by viewModel.alerts.collectAsStateWithLifecycle()
            val assets by viewModel.assets.collectAsStateWithLifecycle()
            val lightningInfo by viewModel.lightningInfo.collectAsStateWithLifecycle()
            val transactions by viewModel.transactions.collectAsStateWithLifecycle()
            val hasMoreTransactions by viewModel.hasMoreTransactions.collectAsStateWithLifecycle()

            val innerPadding = LocalInnerPadding.current
            val listState: LazyListState = rememberLazyListState()
            val reachedBottom: Boolean by remember { derivedStateOf { listState.reachedBottom() } }

            LaunchedEffect(reachedBottom) {
                if (reachedBottom && hasMoreTransactions && transactions.isSuccess()) {
                    viewModel.postEvent(AccountOverviewViewModel.LocalEvents.LoadMoreTransactions)
                }
            }

            LazyColumn(
                contentPadding = innerPadding
                    .bottom(), state = listState
            ) {

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
                                if (accountBalance.hasExpiredUtxos) {
                                    viewModel.postEvent(NavigateDestinations.ReEnable2FA(greenWallet = viewModel.greenWallet))
                                } else {
                                    viewModel.postEvent(
                                        NavigateDestinations.EnableTwoFactor(
                                            greenWallet = viewModel.greenWallet,
                                            network = accountBalance.account.network
                                        )
                                    )
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
                                        greenWallet = viewModel.greenWallet,
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
                                    greenWallet = viewModel.greenWallet,
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
                    items(
                        items = it,
                        key = { it.transaction.txHash.hashCode() + it.transaction.txType.gdkType.hashCode() }) {
                        GreenTransaction(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp), transactionLook = it) {
                            viewModel.postEvent(Events.Transaction(transaction = it.transaction))
                        }
                    }
                }
            }
        }
    }
}