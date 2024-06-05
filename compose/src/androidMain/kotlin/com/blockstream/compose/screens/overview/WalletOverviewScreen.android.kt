package com.blockstream.compose.screens.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.overview.WalletOverviewViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
fun WalletBalancePreview() {
    GreenThemePreview {
        WalletBalance(viewModel = WalletOverviewViewModelPreview.create())
    }
}

@Composable
@Preview
fun WalletAssetsPreview() {
    GreenThemePreview {
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