package com.blockstream.compose.screens.overview

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.overview.WalletOverviewViewModel
import com.blockstream.common.models.overview.WalletOverviewViewModelAbstract
import com.blockstream.common.models.overview.WalletOverviewViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.screens.transaction.TransactionScreen
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import org.koin.core.parameter.parametersOf


@Parcelize
data class WalletOverviewScreen(
    val greenWallet: GreenWallet,
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<WalletOverviewViewModel>() {
            parametersOf(greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        WalletOverviewScreen(viewModel = viewModel)
    }

    @IgnoredOnParcel
    override val key = uniqueScreenKey
}

@Composable
fun WalletOverviewScreen(
    viewModel: WalletOverviewViewModelAbstract
) {
    HandleSideEffect(viewModel = viewModel)

    val navigator = LocalNavigator.current

    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            // fetch something
            delay(1500)
            state.endRefresh()
        }
    }

    Box(
        Modifier.nestedScroll(state.nestedScrollConnection)
    ) {

        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
        val transactions by viewModel.transactions.collectAsStateWithLifecycle()

        LazyColumn {
            item {
                GreenColumn {
                    Column(modifier = Modifier.clickable {
                        viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ToggleBalance)
                    }) {
                        val balancePrimary by viewModel.balancePrimary.collectAsStateWithLifecycle()
                        Text("Primary Balance: $balancePrimary")
                        val balanceSecondary by viewModel.balanceSecondary.collectAsStateWithLifecycle()
                        Text("Secondary Balance: $balanceSecondary")
                    }

                    val hideAmounts by viewModel.hideAmounts.collectAsStateWithLifecycle()
                    Text("HideAmounts($hideAmounts)", modifier = Modifier.clickable {
                        viewModel.postEvent(WalletOverviewViewModel.LocalEvents.ToggleHideAmounts)
                    })
                }
            }

            item {
                Text(text = "ASSETS:")
            }

            item {
                val assets by viewModel.assets.collectAsStateWithLifecycle()
                Box {
                    assets.forEachIndexed { index, asset ->
                        val size = 24
                        val padding = (size / (1.5 + (0.1 * index)) * index)

                        Image(
                            painter = asset.assetId.assetIcon(session = viewModel.sessionOrNull),
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
            }

            item {
                Text(text = "ACCOUNTS:")
            }

            items(accounts) {
                Text("${it.name}")
            }

            item {
                Text(text = stringResource(R.string.id_latest_transactions), style = titleSmall)
            }

            items(transactions){
                GreenCard {
                    Text(text = it.txHash, modifier = Modifier.clickable {
                        navigator?.push(
                            TransactionScreen(
                                transaction = it,
                                greenWallet = viewModel.greenWallet
                            )
                        )
                    })
                }
            }
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )
    }
}

@Composable
@Preview
fun WalletOverviewPreview() {
    GreenPreview {
        WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.create())
    }
}