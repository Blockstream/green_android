package com.blockstream.compose.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.GreenPreview

@Preview(widthDp = 200, heightDp = 400)
@Preview(widthDp = 300, heightDp = 500)
@Preview
@Composable
fun PinViewPreview() {
    GreenPreview {
        Box {
            PinView(
                isSmall = true,
                showDigits = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 400)
@Preview(widthDp = 300, heightDp = 500)
@Preview
@Composable
fun PinViewPreviewSmall() {
    GreenPreview {
        Box {
            PinView(
                isSmall = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
        }
    }
}