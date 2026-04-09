package com.blockstream.compose.screens.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import blockstream_green.common.generated.resources.id_latest_transactions
import blockstream_green.common.generated.resources.id_your_transactions_will_be_shown
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.ListHeader
import com.blockstream.compose.components.TransactionActionButtons
import com.blockstream.compose.components.WalletBalance
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.overview.TransactViewModelAbstract
import com.blockstream.compose.models.overview.TransactViewModelPreview
import com.blockstream.compose.navigation.LocalInnerPadding
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.SwapUtils
import com.blockstream.compose.utils.bottom
import com.blockstream.compose.utils.plus
import com.blockstream.compose.utils.reachedBottom
import com.blockstream.data.data.GreenWallet
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun TransactScreen(viewModel: TransactViewModelAbstract) {

    NavigateDestinations.Login.getResult<GreenWallet> {
        SwapUtils.navigateToDeviceScanOrJadeQr(viewModel)
    }

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.resetTransactionList()
            delay(1.toDuration(DurationUnit.SECONDS))
            isRefreshing = false
        }
    }

    SetupScreen(viewModel = viewModel, withPadding = false, withBottomInsets = false) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
            },
        ) {

            val isMainnet = viewModel.greenWallet.isMainnet
            val isSwapAvailable = viewModel.isSwapAvailable
            val transactions by viewModel.transactions.collectAsStateWithLifecycle()
            val isMultisigWatchOnly by viewModel.isMultisigWatchOnly.collectAsStateWithLifecycle()
            val innerPadding = LocalInnerPadding.current

            val listState: LazyListState = rememberLazyListState()
            val reachedBottom: Boolean by remember { derivedStateOf { listState.reachedBottom() } }

            val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
            val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

            LaunchedEffect(reachedBottom, hasMore, isLoadingMore) {
                if (reachedBottom && hasMore && transactions.isSuccess() && !isLoadingMore) {
                    viewModel.onLoadMore()
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = innerPadding.bottom()
                    .plus(PaddingValues(horizontal = 16.dp))
                    .plus(PaddingValues(bottom = 80.dp + 16.dp)),
            ) {

                item(key = "WalletBalance") {
                    WalletBalance(viewModel = viewModel)
                }

                item(key = "ButtonsRow") {
                    TransactionActionButtons(
                        modifier = Modifier.padding(top = 16.dp),
                        showBuyButton = isMainnet,
                        showSwapButton = isSwapAvailable,
                        isSendEnabled = !isMultisigWatchOnly,
                        onBuy = viewModel::onBuy,
                        onSend = viewModel::onSend,
                        onReceive = viewModel::onReceive,
                        onSwap = viewModel::onSwap
                    )
                }

                item(key = "TransactionsHeader") {
                    ListHeader(title = stringResource(Res.string.id_latest_transactions))

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
                    tx.transaction.uniqueId
                }) { item ->
                    GreenTransaction(modifier = Modifier.padding(bottom = 6.dp), transactionLook = item) {
                        viewModel.postEvent(Events.Transaction(transaction = it.transaction))
                    }
                }

                if (isLoadingMore) {
                    item(key = "PaginationLoading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewTransactScreen() {
    GreenPreview {
        TransactScreen(viewModel = TransactViewModelPreview.create())
    }
}