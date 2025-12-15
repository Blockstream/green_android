package com.blockstream.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_my_wallets
import blockstream_green.common.generated.resources.id_set_up_a_new_wallet
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.data.data.GreenWallet
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.Promo
import com.blockstream.compose.looks.wallet.WalletListLook
import com.blockstream.compose.models.home.HomeViewModel
import com.blockstream.compose.models.home.HomeViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.fadingEdges
import com.blockstream.compose.views.WalletListItem
import com.blockstream.compose.views.WalletListItemCallbacks
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

open class WalletSectionCallbacks constructor(
    onWalletClick: (wallet: GreenWallet) -> Unit,
    onWalletDelete: ((wallet: GreenWallet) -> Unit),
    onWalletRename: ((wallet: GreenWallet) -> Unit),
    hasContextMenu: Boolean = false
) : WalletListItemCallbacks(
    onWalletClick = onWalletClick,
    onWalletDelete = onWalletDelete,
    onWalletRename = onWalletRename,
    hasContextMenu = hasContextMenu
)

private fun LazyListScope.walletSection(
    title: StringResource,
    wallets: List<WalletListLook>,
    callbacks: WalletListItemCallbacks,
) {
    item {
        Text(
            text = stringResource(title),
            style = titleMedium,
//            modifier = Modifier.padding(top = 8.dp)
        )
    }
    items(wallets) { item ->
        WalletListItem(look = item, callbacks = callbacks)
    }
}

@Composable
fun WalletsScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModelAbstract,
) {
    val isEmptyWallet by viewModel.isEmptyWallet.collectAsStateWithLifecycle()
    val allWallets by viewModel.allWallets.collectAsStateWithLifecycle()

    val callbacks = WalletSectionCallbacks(onWalletClick = { wallet ->
        viewModel.postEvent(
            HomeViewModel.LocalEvents.SelectWallet(
                greenWallet = wallet
            )
        )
    }, onWalletDelete = {
        viewModel.postEvent(NavigateDestinations.DeleteWallet(it))
    }, onWalletRename = {
        viewModel.postEvent(NavigateDestinations.RenameWallet(it))
    }, hasContextMenu = true)

    Box {

        GreenColumn(
            space = 8,
            padding = 0,
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier)
        ) {

            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fadingEdges(lazyListState)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Promo(viewModel = viewModel, modifier = Modifier.padding(top = 16.dp))
                }

                allWallets?.takeIf { it.isNotEmpty() }?.also {
                    walletSection(
                        title = Res.string.id_my_wallets,
                        wallets = it,
                        callbacks = callbacks
                    )
                }
            }

            if (isEmptyWallet == false) {
                GreenCard(testTag = "id_setup_a_new_wallet", onClick = {
                    viewModel.postEvent(NavigateDestinations.GetStarted)
                }, modifier = Modifier.padding(bottom = 16.dp), padding = 0) {
                    Row(
                        modifier = Modifier
                            .height(60.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.id_set_up_a_new_wallet),
                            modifier = Modifier.weight(1f),
                            style = labelMedium
                        )

                        Icon(
                            imageVector = PhosphorIcons.Regular.CaretRight,
                            contentDescription = null,
                            tint = whiteLow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
