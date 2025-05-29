package com.blockstream.compose.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.settings.DenominationExchangeRateViewModelPreview
import com.blockstream.compose.theme.GreenChromePreview

@Composable
@Preview
fun DenominationExchangeDialogPreview() {
    GreenChromePreview {
        DenominationExchangeDialog(
            viewModel = DenominationExchangeRateViewModelPreview.preview()
        ) {

        }
    }
}