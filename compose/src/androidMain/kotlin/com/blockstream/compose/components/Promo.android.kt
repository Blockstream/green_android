package com.blockstream.compose.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.GreenPreview
import com.blockstream.data.data.Promo

@Composable
@Preview
fun PromoPreview() {
    GreenPreview {
        GreenColumn(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            PromoCard(
                promo = Promo.preview1,
                onDismiss = {},
                onAction = {},
            )
        }
    }
}
