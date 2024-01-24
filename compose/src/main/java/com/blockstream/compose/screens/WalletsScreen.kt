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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Events
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.common.models.wallets.WalletsViewModelAbstract
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.common.views.wallet.WalletListLook
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.views.WalletListItem
import com.blockstream.compose.views.WalletListItemCallbacks

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
    title: Int,
    wallets: List<WalletListLook>,
    callbacks: WalletListItemCallbacks,
) {
    item {
        Text(
            text = stringResource(id = title),
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
        viewModel.postEvent(WalletsViewModel.LocalEvents.RemoveLightningShortcut(it))
    }, onWalletDelete = {
        viewModel.postEvent(Events.ShowDeleteWallet(it))
    }, onWalletRename = {
        viewModel.postEvent(Events.ShowRenameWallet(it))
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
                    title = R.string.id_digital_wallets,
                    wallets = it,
                    callbacks = callbacks
                )
            }

            ephemeralWallets?.takeIf { it.isNotEmpty() }?.also {
                walletSection(
                    title = R.string.id_ephemeral_wallets,
                    wallets = it,
                    callbacks = callbacks
                )
            }

            hardwareWallets?.takeIf { it.isNotEmpty() }?.also {
                walletSection(
                    title = R.string.id_hardware_devices,
                    wallets = it,
                    callbacks = callbacks
                )
            }
        }

        if(isEmptyWallet == false) {
            Card {
                Row(modifier = Modifier
                    .clickable {
                        viewModel.postEvent(Events.SetupNewWallet)
                    }
                    .height(52.dp)
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.id_setup_a_new_wallet),
                        modifier = Modifier.weight(1f),
                        style = labelMedium
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.caret_right),
                        contentDescription = null,
                        tint = whiteLow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

class NameProvider : PreviewParameterProvider<WalletsViewModelPreview> {
    override val values: Sequence<WalletsViewModelPreview> = sequenceOf(
        WalletsViewModelPreview.previewEmpty(),
        WalletsViewModelPreview.previewSoftwareOnly(),
        WalletsViewModelPreview.previewHardwareOnly(),
        WalletsViewModelPreview.previewAll()
    )
}

@Composable
@Preview()
fun WalletsScreenPreview(
    @PreviewParameter(NameProvider::class) viewModel: WalletsViewModelPreview
) {
    GreenPreview {
        WalletsScreen(viewModel = viewModel)
    }
}