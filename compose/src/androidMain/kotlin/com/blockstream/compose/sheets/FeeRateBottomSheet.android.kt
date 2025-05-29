package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.FeePriority
import com.blockstream.common.models.send.FeeViewModelPreview
import com.blockstream.common.utils.feeRateWithUnit
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.ui.components.GreenColumn

@Composable
@Preview
fun FeeItemPreview() {
    GreenChromePreview {
        GreenColumn {
            FeeItem(
                FeePriority.High(
                    fee = "0,0001235 BTC",
                    feeFiat = "~ 45,42 USD",
                    feeRate = 2345L.feeRateWithUnit(),
                    expectedConfirmationTime = "~10 Minutes"
                )
            )

            FeeItem(
                FeePriority.Medium(
                    fee = "0,0000235 BTC",
                    feeFiat = "~ 40,42 USD",
                    feeRate = 1234L.feeRateWithUnit(),
                    error = "id_insufficient_funds",
                    expectedConfirmationTime = "~30 Minutes"
                )
            )
        }
    }
}

@Composable
@Preview
fun FeeRateBottomSheetPreview() {
    GreenAndroidPreview {
        FeeRateBottomSheet(
            viewModel = FeeViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}