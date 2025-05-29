package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.wallet.WalletDeleteViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.ui.components.GreenColumn

@ExperimentalMaterial3Api
@Composable
@Preview
fun WalletDeleteBottomSheetPreview() {
    GreenAndroidPreview {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            Text("WalletRenameBottomSheet")

            if (showBottomSheet) {
                WalletDeleteBottomSheet(
                    viewModel = WalletDeleteViewModelPreview.preview(),
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}