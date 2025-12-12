package com.blockstream.compose.screens.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.models.overview.AccountOverviewViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AccountOverviewPreview() {
    GreenAndroidPreview {
        AccountOverviewScreen(viewModel = AccountOverviewViewModelPreview.create())
    }
}
