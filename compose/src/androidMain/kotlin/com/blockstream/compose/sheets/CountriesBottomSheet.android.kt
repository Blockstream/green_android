package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun CountriesBottomSheetPreview() {
    GreenAndroidPreview {
        CountriesBottomSheet(
            viewModel = GreenViewModel.preview(),
            title = "Billing",
            subtitle = "Please select your correct billing location to complete the checkout successfully.",
            showDialCode = false,
            onDismissRequest = { }
        )
    }
}