package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.ui.components.GreenArrow
import com.blockstream.ui.components.GreenColumn

@Composable
@Preview
fun GreenArrowPreview() {
    GreenChromePreview {
        GreenColumn {
            GreenArrow()
            GreenArrow(enabled = false)
        }
    }
}