package com.blockstream.compose.screens.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.overview.WalletAssetsViewModel
import com.blockstream.common.models.overview.WalletAssetsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import org.koin.core.parameter.parametersOf


@Parcelize
data class WalletAssetsScreen(
    val greenWallet: GreenWallet,
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WalletAssetsViewModel>() {
            parametersOf(greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        WalletAssetsScreen(viewModel = viewModel)
    }
}

@Composable
fun WalletAssetsScreen(
    viewModel: WalletAssetsViewModelAbstract
) {

    HandleSideEffect(viewModel = viewModel)

    val assets by viewModel.assets.collectAsStateWithLifecycle()

    LazyColumn {
        item {
            if (assets.isLoading()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .height(1.dp)
                        .fillMaxWidth()
                )
            }
        }

        assets.data()?.also {
            items(it) {
                GreenAsset(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 2.dp),
                    assetBalance = it,
                    session = viewModel.sessionOrNull
                ) {
                    viewModel.postEvent(
                        NavigateDestinations.AssetDetails(
                            assetId = it.assetId
                        )
                    )
                }
            }
        }
    }
}