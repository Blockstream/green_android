package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.data.Denomination
import com.blockstream.common.models.send.DenominationViewModelPreview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.GreenThemePreview

@Composable
@Preview
fun DenominatedValueItemPreview() {
    GreenThemePreview {
        Column {
            DenominatedValueItem(DenominatedValue(Denomination.BTC))
            DenominatedValueItem(DenominatedValue(Denomination.SATOSHI), isChecked = true)
        }
    }
}

@Composable
@Preview
fun DenominationBottomSheetPreview() {
    GreenAndroidPreview {
        DenominationBottomSheet(
            viewModel = DenominationViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}