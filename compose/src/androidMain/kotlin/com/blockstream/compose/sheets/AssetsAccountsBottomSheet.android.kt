package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.extensions.previewAccount
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.compose.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AssetsAccountBottomSheetPreview() {
    GreenAndroidPreview {
        AssetsAccountsBottomSheet(
            viewModel = SimpleGreenViewModelPreview(previewWallet()),
            assetsAccounts = listOf(previewAccount()).map {
                AccountAssetBalance.create(accountAsset = it.accountAsset)
            }.let { AccountAssetBalanceList(it) },
            onDismissRequest = { }
        )
    }
}