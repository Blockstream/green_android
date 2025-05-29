package com.blockstream.compose.screens.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.overview.WalletOverviewViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.WalletBalance

@Composable
@Preview
fun WalletBalancePreview() {
    GreenAndroidPreview {
        WalletBalance(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletAssetsPreview() {
    GreenAndroidPreview {
        // WalletAssets(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletOverviewPreview() {
    GreenAndroidPreview {
        WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletOverviewEmptyPreview() {
    GreenAndroidPreview {
        WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.create(isEmpty = true))
    }
}