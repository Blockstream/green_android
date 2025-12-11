package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.theme.GreenChromePreview

@Preview
@Composable
fun GreenSearchFieldPreview() {
    GreenChromePreview {
        GreenColumn {
            var text by remember {
                mutableStateOf("")
            }
            GreenSearchField(text, {
                text = it
            })
        }
    }
}