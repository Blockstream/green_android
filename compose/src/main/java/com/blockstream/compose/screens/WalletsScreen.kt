package com.blockstream.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.wallets.WalletsViewModelAbstract
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.common.views.wallet.WalletListLook
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.views.WalletListItem
import com.blockstream.compose.views.WalletListItemCallbacks

open class WalletSectionCallbacks(
    onWalletClick: (wallet: GreenWallet, isLightning: Boolean) -> Unit = { _, _ -> },
    onWalletRename: ((wallet: GreenWallet) -> Unit)? = null,
    onWalletDelete: ((wallet: GreenWallet) -> Unit)? = null,
    val onNewWalletClick: () -> Unit = {}
) : WalletListItemCallbacks(onWalletClick = onWalletClick, onWalletRename = onWalletRename , onWalletDelete = onWalletDelete)

@Composable
private fun WalletSection(
    title: String,
    wallets: List<WalletListLook>,
    callbacks: WalletListItemCallbacks
) {
    GreenColumn(padding = 0, space = 4) {
        Text(title, style = labelMedium)

        wallets.forEach {
            WalletListItem(look = it, callbacks = callbacks)
        }

        GreenSpacer(0)
    }
}

@Composable
fun WalletsScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletsViewModelAbstract,
    callbacks: WalletSectionCallbacks = WalletSectionCallbacks()
) {
    val softwareWallets by viewModel.softwareWallets.collectAsState()
    val ephemeralWallets by viewModel.ephemeralWallets.collectAsState()
    val hardwareWallets by viewModel.hardwareWallets.collectAsState()

    GreenColumn(
        space = 8,
        padding = 0,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        GreenColumn(
            space = 16,
            padding = 0,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 8.dp),
        ) {

            if (!softwareWallets.isNullOrEmpty()) {
                WalletSection(
                    title = stringResource(id = R.string.id_digital_wallets),
                    wallets = softwareWallets!!,
                    callbacks = callbacks
                )
            }

            if (!ephemeralWallets.isNullOrEmpty()) {
                WalletSection(
                    title = stringResource(id = R.string.id_ephemeral_wallets),
                    wallets = ephemeralWallets!!,
                    callbacks = callbacks
                )
            }

            if (!hardwareWallets.isNullOrEmpty()) {
                WalletSection(
                    title = stringResource(id = R.string.id_hardware_devices),
                    wallets = hardwareWallets!!,
                    callbacks = callbacks
                )
            }
        }

        Card {
            Row(modifier = Modifier
                .clickable {
                    callbacks.onNewWalletClick.invoke()
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
    GreenTheme {
        GreenColumn {
            WalletsScreen(viewModel = viewModel)
        }
    }
}