@file:OptIn(ExperimentalFoundationApi::class)

package com.blockstream.compose.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.caret_right
import blockstream_green.common.generated.resources.id_digital_wallets
import blockstream_green.common.generated.resources.id_ephemeral_wallets
import blockstream_green.common.generated.resources.id_hardware_devices
import blockstream_green.common.generated.resources.id_setup_a_new_wallet
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.looks.wallet.WalletListLook
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.common.models.wallets.WalletsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.views.WalletListItem
import com.blockstream.compose.views.WalletListItemCallbacks
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

open class WalletSectionCallbacks constructor(
    onWalletClick: (wallet: GreenWallet, isLightning: Boolean) -> Unit,
    onLightningShortcutDelete: ((wallet: GreenWallet) -> Unit),
    onWalletDelete: ((wallet: GreenWallet) -> Unit),
    onWalletRename: ((wallet: GreenWallet) -> Unit),
    hasContextMenu: Boolean = false
) : WalletListItemCallbacks(
    onWalletClick = onWalletClick,
    onLightningShortcutDelete = onLightningShortcutDelete,
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
            style = labelMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
    items(wallets) { item ->
        WalletListItem(look = item, callbacks = callbacks)
    }
}

@Composable
fun WalletsScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletsViewModelAbstract,
) {
    val isEmptyWallet by viewModel.isEmptyWallet.collectAsStateWithLifecycle()
    val softwareWallets by viewModel.softwareWallets.collectAsStateWithLifecycle()
    val ephemeralWallets by viewModel.ephemeralWallets.collectAsStateWithLifecycle()
    val hardwareWallets by viewModel.hardwareWallets.collectAsStateWithLifecycle()

    val callbacks = WalletSectionCallbacks(onWalletClick = { wallet, isLightningShortcut ->
        viewModel.postEvent(
            WalletsViewModel.LocalEvents.SelectWallet(
                greenWallet = wallet,
                isLightningShortcut = isLightningShortcut
            )
        )
    }, onLightningShortcutDelete = {
        viewModel.postEvent(Events.AskRemoveLightningShortcut(wallet = it))
    }, onWalletDelete = {
        viewModel.postEvent(NavigateDestinations.DeleteWallet(it))
    }, onWalletRename = {
        viewModel.postEvent(NavigateDestinations.RenameWallet(it))
    }, hasContextMenu = viewModel.isHome)

    GreenColumn(
        space = 8,
        padding = 0,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {

        LazyColumn(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            softwareWallets?.takeIf { it.isNotEmpty() }?.also {
                walletSection(
                    title = Res.string.id_digital_wallets,
                    wallets = it,
                    callbacks = callbacks
                )
            }

            ephemeralWallets?.takeIf { it.isNotEmpty() }?.also {
                walletSection(
                    title = Res.string.id_ephemeral_wallets,
                    wallets = it,
                    callbacks = callbacks
                )
            }

            hardwareWallets?.takeIf { it.isNotEmpty() }?.also {
                walletSection(
                    title = Res.string.id_hardware_devices,
                    wallets = it,
                    callbacks = callbacks
                )
            }
        }

        if(isEmptyWallet == false) {
            Card {
                Row(modifier = Modifier
                    .clickable {
                        viewModel.postEvent(NavigateDestinations.SetupNewWallet)
                    }
                    .height(52.dp)
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.id_setup_a_new_wallet),
                        modifier = Modifier.weight(1f),
                        style = labelMedium
                    )

                    Icon(
                        painter = painterResource(Res.drawable.caret_right),
                        contentDescription = null,
                        tint = whiteLow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
