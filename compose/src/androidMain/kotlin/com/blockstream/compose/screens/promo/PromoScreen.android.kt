package com.blockstream.compose.screens.promo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.promo.PromoViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun PromoScreenPreview() {
    GreenAndroidPreview {
        PromoScreen(viewModel = PromoViewModelPreview())
    }
}
