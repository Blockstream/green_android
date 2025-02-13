package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.models.SimpleGreenViewModelPreview
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