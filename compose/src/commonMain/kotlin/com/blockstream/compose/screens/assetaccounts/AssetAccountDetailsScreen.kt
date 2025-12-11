package com.blockstream.compose.screens.assetaccounts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_transactions
import blockstream_green.common.generated.resources.id_your_transactions_will_be_shown
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.assetaccounts.AssetAccountDetailsViewModel
import com.blockstream.common.models.assetaccounts.AssetAccountDetailsViewModelAbstract
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.ListHeader
import com.blockstream.compose.components.TransactionActionButtons
import com.blockstream.compose.extensions.itemsSpaced
import com.blockstream.compose.screens.assetaccounts.components.AssetOverview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.views.LightningInfo
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.utils.bottom
import com.blockstream.compose.utils.plus
import com.blockstream.compose.utils.reachedBottom
import org.jetbrains.compose.resources.stringResource

@Composable
fun AssetAccountDetailsScreen(
    viewModel: AssetAccountDetailsViewModelAbstract
) {
    SetupScreen(viewModel = viewModel, withPadding = false, withBottomInsets = false) {

        val asset = viewModel.asset
        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
        val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
        val totalBalanceFiat by viewModel.totalBalanceFiat.collectAsStateWithLifecycle()
        val showBuyButton = viewModel.showBuyButton
        val isSendEnabled by viewModel.isSendEnabled.collectAsStateWithLifecycle()
        val hasMoreTransactions by viewModel.hasMoreTransactions.collectAsStateWithLifecycle()
        val lightningInfo by viewModel.lightningInfo.collectAsStateWithLifecycle()
        
        val innerPadding = LocalInnerPadding.current
        val listState: LazyListState = rememberLazyListState()
        val reachedBottom: Boolean by remember { derivedStateOf { listState.reachedBottom() } }
        
        LaunchedEffect(reachedBottom) {
            if (reachedBottom && hasMoreTransactions && transactions.isSuccess()) {
                viewModel.postEvent(AssetAccountDetailsViewModel.LocalEvents.LoadMoreTransactions)
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = innerPadding.bottom()
                .plus(PaddingValues(horizontal = 16.dp))
                .plus(PaddingValues(bottom = 80.dp + 16.dp)),
        ) {
            
            item(key = "AssetOverview") {
                AssetOverview(
                    asset = asset,
                    totalBalance = totalBalance,
                    totalBalanceFiat = totalBalanceFiat,
                    session = viewModel.sessionOrNull
                )
            }
            
            item { Spacer(Modifier.height(32.dp)) }

            item(key = "ButtonsRow") {
                TransactionActionButtons(
                    showBuyButton = showBuyButton,
                    sendEnabled = isSendEnabled,
                    onBuy = { viewModel.postEvent(AssetAccountDetailsViewModel.LocalEvents.ClickBuy) },
                    onSend = { viewModel.postEvent(AssetAccountDetailsViewModel.LocalEvents.ClickSend) },
                    onReceive = { viewModel.postEvent(AssetAccountDetailsViewModel.LocalEvents.ClickReceive) }
                )
            }

            lightningInfo?.takeIf { it.sweep.isNotBlank() }?.also { lightningInfo ->
                item(key = "LightningInfo") {
                    LightningInfo(lightningInfoLook = lightningInfo, showCapacity = false, onSweepClick = {
                        viewModel.clickLightningSweep()
                    })
                }
            }

            item(key = "TransactionsHeader") {
                ListHeader(
                    title = stringResource(Res.string.id_transactions),
                )
            }

            if (transactions.isLoading()) {
                item(key = "TransactionsLoading") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                    )
                }
            } else if (transactions.isEmpty()) {
                item(key = "NoTransactions") {
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

            transactions.data()?.let { transactionList ->
                itemsSpaced(
                    items = transactionList,
                    key = { _, tx -> 
                        tx.transaction.account.id.hashCode() + 
                        tx.transaction.txHash.hashCode() + 
                        tx.transaction.txType.gdkType.hashCode()
                    }
                ) { transactionLook ->
                    GreenTransaction(
                        modifier = Modifier, 
                        transactionLook = transactionLook
                    ) {
                        viewModel.postEvent(Events.Transaction(transaction = it.transaction))
                    }
                }
            }
        }
    }
}