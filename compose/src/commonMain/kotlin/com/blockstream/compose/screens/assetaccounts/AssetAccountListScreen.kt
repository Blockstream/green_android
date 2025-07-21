package com.blockstream.compose.screens.assetaccounts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_accounts
import blockstream_green.common.generated.resources.id_transactions
import blockstream_green.common.generated.resources.id_your_transactions_will_be_shown
import com.blockstream.common.events.Events
import com.blockstream.common.models.assetaccounts.AssetAccountListViewModel
import com.blockstream.common.models.assetaccounts.AssetAccountListViewModelAbstract
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.ListHeader
import com.blockstream.compose.extensions.itemsSpaced
import com.blockstream.compose.screens.assetaccounts.components.AssetOverview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun AssetAccountListScreen(
    viewModel: AssetAccountListViewModelAbstract
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val asset by viewModel.asset.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalBalanceFiat by viewModel.totalBalanceFiat.collectAsStateWithLifecycle()
//    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "AssetOverview") {
                AssetOverview(
                    asset = asset, totalBalance = totalBalance, totalBalanceFiat = totalBalanceFiat, session = viewModel.sessionOrNull
                )
            }
            item { Spacer(Modifier.height(8.dp)) }

            item(key = "AccountsHeader") {
                ListHeader(title = stringResource(Res.string.id_accounts))
            }

            if (isLoading) {
                item(key = "Loading") {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
                    )
                }
            } else {
                itemsSpaced(
                    items = accounts, key = { _, item -> item.account.id }) { accountAssetBalance ->
                    GreenAccountAsset(
                        accountAssetBalance = accountAssetBalance,
                        session = viewModel.sessionOrNull,
                        withAsset = false,
                        withAssetIcon = false,
                    ) {
                        viewModel.postEvent(AssetAccountListViewModel.LocalEvents.AccountClick(accountAssetBalance))
                    }
                }
            }

//            item(key = "TransactionsHeader") {
//                ListHeader(
//                    title = stringResource(Res.string.id_transactions),
//                )
//            }
//
//            if (transactions.isLoading()) {
//                item(key = "TransactionsLoading") {
//                    LinearProgressIndicator(
//                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
//                    )
//                }
//            } else if (transactions.isEmpty()) {
//                item(key = "NoTransactions") {
//                    Text(
//                        text = stringResource(Res.string.id_your_transactions_will_be_shown),
//                        style = bodyMedium,
//                        textAlign = TextAlign.Center,
//                        fontStyle = FontStyle.Italic,
//                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp).padding(horizontal = 16.dp)
//                    )
//                }
//            }
//
//            transactions.data()?.let { transactionList ->
//                itemsSpaced(
//                    items = transactionList,
//                    key = { _, tx -> tx.transaction.txHash.hashCode() + tx.transaction.txType.gdkType.hashCode() }) { transactionLook ->
//                    GreenTransaction(
//                        modifier = Modifier, transactionLook = transactionLook
//                    ) {
//                        viewModel.postEvent(Events.Transaction(transaction = it.transaction))
//                    }
//                }
//            }

        }
    }
}

