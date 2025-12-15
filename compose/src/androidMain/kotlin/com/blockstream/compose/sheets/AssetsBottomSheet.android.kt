package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.extensions.previewAssetBalance
import com.blockstream.data.gdk.data.AssetBalanceList
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun AssetsBottomSheetPreview() {
    GreenAndroidPreview {
        AssetsBottomSheet(
            viewModel = GreenViewModel.preview(),
            assetBalance = AssetBalanceList(
                listOf(
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance(),
                    previewAssetBalance()
                )
            ),
            onDismissRequest = { }
        )
    }
}