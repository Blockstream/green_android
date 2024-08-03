package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewAccountAsset
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun AccountRenameBottomSheetPreview() {
    GreenAndroidPreview {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            Text("WalletRenameBottomSheet")

            if (showBottomSheet) {
                AccountRenameBottomSheet(
                    viewModel = SimpleGreenViewModelPreview(previewWallet(), previewAccountAsset()),
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}