package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.gdk.data.UtxoView
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenAndroidPreview


@Composable
@Preview
fun VerifyOnDeviceTransactionBottomSheetPreview() {
    GreenAndroidPreview {
        VerifyOnDeviceBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            address = null,
            transactionConfirmLook = TransactionConfirmLook(
                utxos = listOf(
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        isChange = true,
                        amount = "1 BTC"
                    ),
                    UtxoView(
                        address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu",
                        isChange = false,
                        amount = "2 BTC"
                    )
                ),
                fee = "1 BTC"
            ),
            onDismissRequest = { }
        )
    }
}

@Composable
@Preview
fun VerifyOnDeviceAddressBottomSheetPreview() {
    GreenAndroidPreview {
        VerifyOnDeviceBottomSheet(
            viewModel = SimpleGreenViewModelPreview(),
            address = "bc1tinyaddresstestonly",
            onDismissRequest = { }
        )
    }
}