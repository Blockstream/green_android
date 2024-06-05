package com.blockstream.compose.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.blockstream.common.models.wallets.WalletsViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

class NameProvider : PreviewParameterProvider<WalletsViewModelPreview> {
    override val values: Sequence<WalletsViewModelPreview> = sequenceOf(
        WalletsViewModelPreview.previewEmpty(),
        WalletsViewModelPreview.previewSoftwareOnly(),
        WalletsViewModelPreview.previewHardwareOnly(),
        WalletsViewModelPreview.previewAll()
    )
}

@Composable
@Preview()
fun WalletsScreenPreview(
    @PreviewParameter(NameProvider::class) viewModel: WalletsViewModelPreview
) {
    GreenAndroidPreview {
        WalletsScreen(viewModel = viewModel)
    }
}