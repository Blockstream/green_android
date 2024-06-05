package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
fun GreenArrowPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenArrow()
            GreenArrow(enabled = false)
        }
    }
}