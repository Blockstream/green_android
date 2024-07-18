package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.overview.WalletOverviewViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenButton

@Composable
@Preview
fun OverviewMenuDialogPreview() {
    GreenAndroidPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            var walletOverviewViewModel by remember { mutableStateOf<WalletOverviewViewModelPreview?>(WalletOverviewViewModelPreview()) }

            if (walletOverviewViewModel != null) {
                WalletOverviewMenuDialog(viewModel = walletOverviewViewModel!!) {
                    walletOverviewViewModel = null
                }
            }

            GreenButton("Show Dialog") {
                walletOverviewViewModel = WalletOverviewViewModelPreview()
            }
        }
    }
}