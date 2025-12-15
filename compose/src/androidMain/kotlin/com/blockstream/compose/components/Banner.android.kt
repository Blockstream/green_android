package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.GreenPreview
import com.blockstream.data.banner.Banner

@Composable
@Preview
fun BannerPreview() {
    GreenPreview {
        GreenColumn {
            Banner(Banner.preview1)
            Banner(Banner.preview2)
            Banner(Banner.preview3)
        }
    }
}