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
import com.blockstream.common.models.overview.WalletAssetsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.utils.SetupScreen

@Composable
fun WalletAssetsScreen(
    viewModel: WalletAssetsViewModelAbstract
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, withPadding = false) {
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
                                greenWallet = viewModel.greenWallet,
                                assetId = it.assetId
                            )
                        )
                    }
                }
            }
        }
    }
}