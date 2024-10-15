package com.blockstream.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.GreenPreview

@Composable
@Preview
fun ScreenContainerPreview() {
    GreenPreview {
        ScreenContainer(
            onProgress = true,
            riveAnimation = RiveAnimation.ROCKET,
            onProgressDescription = "On Progress Description..."
        ) {
            GreenColumn {
                Text(text = "Text")
                Text(text = "Text")
                Text(text = "Text")
            }
        }
    }
}