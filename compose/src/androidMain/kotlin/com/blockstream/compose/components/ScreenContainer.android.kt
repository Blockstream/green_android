package com.blockstream.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.IOnProgress
import com.blockstream.compose.GreenPreview
import com.blockstream.ui.components.GreenColumn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
@Preview
fun ScreenContainerPreview() {
    GreenPreview {
        ScreenContainer(
            viewModel = object : IOnProgress {
                override val onProgress: StateFlow<Boolean> = MutableStateFlow(true)
                override val onProgressDescription = MutableStateFlow("On Progress Description...")
            },
            onProgressStyle = OnProgressStyle.Full(riveAnimation = RiveAnimation.ROCKET),
        ) {
            GreenColumn {
                Text(text = "Text")
                Text(text = "Text")
                Text(text = "Text")
            }
        }
    }
}