package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.extensions.previewAssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun CountriesBottomSheetPreview() {
    GreenAndroidPreview {
        CountriesBottomSheet(
            viewModel = GreenViewModel.preview(),
            onDismissRequest = { }
        )
    }
}