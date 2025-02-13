package com.blockstream.compose.screens.overview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_buy
import blockstream_green.common.generated.resources.id_latest_transactions
import blockstream_green.common.generated.resources.id_receive
import blockstream_green.common.generated.resources.id_send
import blockstream_green.common.generated.resources.id_your_transactions_will_be_shown
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLineDown
import com.adamglin.phosphoricons.regular.ArrowLineUp
import com.adamglin.phosphoricons.regular.ShoppingCart
import com.blockstream.common.events.Events
import com.blockstream.common.models.overview.TransactViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenTransaction
import com.blockstream.compose.components.ListHeader
import com.blockstream.compose.components.WalletBalance
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.LocalInnerPadding
import com.blockstream.ui.utils.bottom
import com.blockstream.ui.utils.plus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TransactScreen(viewModel: TransactViewModelAbstract) {

    SetupScreen(viewModel = viewModel, withPadding = false, withBottomInsets = false) {

        val transactions by viewModel.transactions.collectAsStateWithLifecycle()
        val innerPadding = LocalInnerPadding.current
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            contentPadding = innerPadding.bottom()
                .plus(PaddingValues(horizontal = 16.dp))
                .plus(PaddingValues(bottom = 80.dp + 16.dp)),
        ) {

            item(key = "WalletBalance") {
                WalletBalance(viewModel = viewModel)
            }

            item(key = "Row") {
                GreenRow(
                    space = 8,
                    padding = 0,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Button(text = Res.string.id_buy, icon = PhosphorIcons.Regular.ShoppingCart) {
                        viewModel.buy()
                    }

                    Button(text = Res.string.id_send, icon = PhosphorIcons.Regular.ArrowLineUp) {
                        viewModel.postEvent(NavigateDestinations.Send(greenWallet = viewModel.greenWallet))
                    }

                    Button(
                        text = Res.string.id_receive,
                        icon = PhosphorIcons.Regular.ArrowLineDown
                    ) {
                        viewModel.postEvent(
                            NavigateDestinations.Receive(
                                greenWallet = viewModel.greenWallet,
                                accountAsset = viewModel.session.activeAccount.value!!.accountAsset
                            )
                        )
                    }
                }
            }

            item(key = "Transactions Header") {
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
                tx.transaction.account.id.hashCode() + tx.transaction.txHash.hashCode() + tx.transaction.txType.gdkType.hashCode()
            }) { item ->
                GreenTransaction(modifier = Modifier.padding(bottom = 6.dp), transactionLook = item) {
                    viewModel.postEvent(Events.Transaction(transaction = it.transaction))
                }
            }
        }
    }
}

@Composable
internal fun RowScope.Button(text: StringResource, icon: ImageVector, onClick: () -> Unit) {
    GreenCard(padding = 24, onClick = onClick, modifier = Modifier.weight(1f)) {
        GreenColumn(
            padding = 0,
            space = 8,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Text(stringResource(text))
        }
    }
}