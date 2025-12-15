package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.extensions.previewAccountAssetBalance
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.compose.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AccountsBottomSheetPreview() {
    GreenAndroidPreview {
        AccountsBottomSheet(
            viewModel = SimpleGreenViewModelPreview(previewWallet()),
            accountsBalance = AccountAssetBalanceList(listOf(previewAccountAssetBalance())),
            withAsset = true,
            onDismissRequest = { }
        )
    }
}