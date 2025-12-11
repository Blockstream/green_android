package com.blockstream.compose.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.data.Promo
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenPreview

@Composable
@Preview
fun PromoPreview() {
    GreenPreview {
        GreenColumn(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
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
            Promo(
                promo = Promo.preview5,
                viewModel = SimpleGreenViewModelPreview()
            )
            Promo(
                promo = Promo.preview6,
                viewModel = SimpleGreenViewModelPreview()
            )
        }
    }
}