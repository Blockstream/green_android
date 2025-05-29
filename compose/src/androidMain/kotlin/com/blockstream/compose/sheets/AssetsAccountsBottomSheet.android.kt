package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.models.SimpleGreenViewModelPreview
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