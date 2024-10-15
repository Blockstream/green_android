@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.blockstream.compose.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.GreenPreview

@ExperimentalMaterial3Api
@Composable
@Preview(showSystemUi = true, showBackground = true)
fun GreenBottomSheetPreview() {
    GreenPreview {
        Text("WalletRenameBottomSheet")

        var showBottomSheet by remember { mutableStateOf(true) }
        GreenBottomSheet(title = "Wallet Name", subtitle = "Change your wallet name", viewModel = null, onDismissRequest = {
            showBottomSheet = false
        }) {
            Text(text = "OK")
        }
    }
}