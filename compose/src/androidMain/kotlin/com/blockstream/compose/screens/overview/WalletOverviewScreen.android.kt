package com.blockstream.compose.screens.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.overview.WalletOverviewViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.WalletBalance
import com.blockstream.compose.theme.GreenChromePreview


@Composable
@Preview
fun WalletBalancePreview() {
    GreenChromePreview {
        WalletBalance(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletAssetsPreview() {
    GreenChromePreview {
        WalletAssets(viewModel = WalletOverviewViewModelPreview.create())
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
        WalletOverviewScreen(viewModel = WalletOverviewViewModelPreview.create(true))
    }
}