package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_bitcoin_is_the_worlds_leading
import blockstream_green.common.generated.resources.id_blockstream_green_supports_both
import blockstream_green.common.generated.resources.id_choose_your_network
import blockstream_green.common.generated.resources.id_the_liquid_network_is_a_bitcoin
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyNetworkViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyNetworkViewModelAbstract
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.components.GreenContentCard
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


data class WatchOnlyNetworkScreen(val setupArgs: SetupArgs) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WatchOnlyNetworkViewModel>() {
            parametersOf(setupArgs)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        WatchOnlyNetworkScreen(viewModel = viewModel)
    }
}

@Composable
fun WatchOnlyNetworkScreen(
    viewModel: WatchOnlyNetworkViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = stringResource(Res.string.id_choose_your_network),
            style = displayMedium,
        )

        Text(
            text = stringResource(Res.string.id_blockstream_green_supports_both),
            style = bodyLarge,
        )
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            val networks by viewModel.networks.collectAsStateWithLifecycle()
            for (network in networks) {
                GreenContentCard(
                    title = network.canonicalName,
                    message = getCaption(network)?.let { stringResource(it) } ?: "",
                    painter = painterResource(network.icon())
                ) {
                    viewModel.postEvent(WatchOnlyNetworkViewModel.LocalEvents.ChooseNetwork(network))
                }
            }
        }
    }
}

private fun getCaption(network: Network): StringResource? {
    return (if (network.isBitcoinMainnet) {
        Res.string.id_bitcoin_is_the_worlds_leading
    } else if (network.isLiquidMainnet) {
        Res.string.id_the_liquid_network_is_a_bitcoin
    } else {
        null
    })
}
