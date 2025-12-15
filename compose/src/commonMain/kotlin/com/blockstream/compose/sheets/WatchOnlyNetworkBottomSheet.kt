package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_multisig_bitcoin
import blockstream_green.common.generated.resources.id_multisig_bitcoin_testnet
import blockstream_green.common.generated.resources.id_multisig_liquid
import blockstream_green.common.generated.resources.id_multisig_liquid_testnet
import blockstream_green.common.generated.resources.id_select_network
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.gdk.data.Network
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.theme.bodyLarge
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchOnlyNetworkBottomSheet(
    viewModel: GreenViewModel,
    setupArgs: SetupArgs,
    onDismissRequest: () -> Unit
) {
    val settingsManager = viewModel.settingsManager
    val appSettings by settingsManager.appSettingsStateFlow.collectAsStateWithLifecycle()
    val testnetEnabled = appSettings.testnet

    val networks = buildList {
        add(viewModel.session.networks.bitcoinGreen)
        add(viewModel.session.networks.liquidGreen)
        if (testnetEnabled) {
            add(viewModel.session.networks.testnetBitcoinGreen)
            add(viewModel.session.networks.testnetLiquidGreen)
        }
    }

    GreenBottomSheet(
        title = stringResource(Res.string.id_select_network),
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = onDismissRequest
    ) {
        GreenColumn(
            modifier = Modifier
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            networks.forEach { network ->
                NetworkItem(
                    networkName = getNetworkDisplayName(network),
                    onClick = {
                        viewModel.postEvent(
                            NavigateDestinations.WatchOnlyMultisig(
                                setupArgs.copy(
                                    isSinglesig = false,
                                    network = network
                                )
                            )
                        )
                        onDismissRequest()
                    }
                )
            }
        }
    }
}

@Composable
private fun NetworkItem(
    networkName: String,
    onClick: () -> Unit
) {
    GreenCard(onClick = onClick) {
        GreenRow(padding = 0) {
            Text(
                text = networkName,
                style = bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = PhosphorIcons.Regular.CaretRight,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun getNetworkDisplayName(network: Network): String {
    return when {
        network.isBitcoinMainnet -> stringResource(Res.string.id_multisig_bitcoin)
        network.isLiquidMainnet -> stringResource(Res.string.id_multisig_liquid)
        network.isBitcoinTestnet -> stringResource(Res.string.id_multisig_bitcoin_testnet)
        network.isLiquidTestnet -> stringResource(Res.string.id_multisig_liquid_testnet)
        else -> network.canonicalName
    }
}