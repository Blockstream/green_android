package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.extensions.previewNetwork
import com.blockstream.data.gdk.data.TwoFactorReset
import com.blockstream.compose.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun TwoFactorResetBottomSheetPreview() {
    GreenAndroidPreview {
        TwoFactorResetBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            network = previewNetwork(),
            twoFactorReset = TwoFactorReset(),
            onDismissRequest = { }
        )
    }
}