package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.Promo
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenPreview

@Composable
@Preview
fun PromoPreview() {
    GreenPreview {
        GreenColumn {
            Promo(
                promo = Promo.preview1,
                viewModel = SimpleGreenViewModelPreview()
            )
            Promo(
                promo = Promo.preview2,
                viewModel = SimpleGreenViewModelPreview()
            )
            Promo(
                promo = Promo.preview3,
                viewModel = SimpleGreenViewModelPreview()
            )
            Promo(
                promo = Promo.preview4,
                viewModel = SimpleGreenViewModelPreview()
            )
        }
    }
}