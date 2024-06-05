package com.blockstream.compose.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_enable_twofactor_authentication
import blockstream_green.common.generated.resources.id_tip_we_recommend_you_enable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.indexOfOrNull
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.settings.TwoFactorAuthenticationViewModel
import com.blockstream.common.models.settings.WalletSettingsSection
import com.blockstream.common.models.settings.WalletSettingsViewModel
import com.blockstream.common.models.settings.WalletSettingsViewModelAbstract
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class TwoFactorAuthenticationScreen(
    val greenWallet: GreenWallet,
    val network: Network? = null
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TwoFactorAuthenticationViewModel> {
            parametersOf(greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        val networkViewModels by remember {
            mutableStateOf(
                viewModel.networks.map {
                    WalletSettingsViewModel(
                        greenWallet = viewModel.greenWallet,
                        section = WalletSettingsSection.TwoFactor,
                        network = it
                    )
                }
            )
        }

        TwoFactorAuthenticationScreen(viewModel = viewModel, networkViewModels = networkViewModels, network = network)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TwoFactorAuthenticationScreen(
    viewModel: TwoFactorAuthenticationViewModel,
    networkViewModels: List<WalletSettingsViewModelAbstract>,
    network: Network? = null
) {

    HandleSideEffect(viewModel)

    Column {
        GreenColumn {
            Text(
                text = stringResource(Res.string.id_enable_twofactor_authentication),
                style = titleSmall,
            )

            Text(
                text = stringResource(Res.string.id_tip_we_recommend_you_enable),
                style = bodyMedium,
                color = whiteMedium,
            )
        }


        val tabs = viewModel.networks.map {
            it.name
        }

        var selectedTabIndex by remember { mutableStateOf(network?.let { viewModel.networks.indexOfOrNull(it) } ?: 0) }

        val pagerState = rememberPagerState(initialPage = network?.let { viewModel.networks.indexOfOrNull(it) } ?: 0) {
            tabs.size
        }

        LaunchedEffect(selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }

        LaunchedEffect(pagerState) {
            // Collect from the a snapshotFlow reading the currentPage
            snapshotFlow { pagerState.currentPage }.collect { page ->
                // Do something with each page change, for example:
                // viewModel.sendPageSelectedEvent(page)
                selectedTabIndex = pagerState.currentPage
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if(tabs.size > 1) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(text = { Text(title) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState, modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { index ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    WalletSettingsScreen(viewModel = networkViewModels[index])
                }
            }
        }
    }
}
