package com.blockstream.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.theme.GreenTheme

@Composable
@Preview
fun GreenQRPreview() {
    GreenTheme {
        GreenColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            GreenQR(
                data = "",
                modifier = Modifier.weight(1f)
            )

            GreenQR(
                data = "chalk verb patch cube sell west penalty fish park worry tribe tourist chalk verb patch cube sell west penalty fish park worry tribe tourist",
                isJadeQR = true,
                modifier = Modifier.fillMaxWidth()
            )

            GreenQR(
                data = "chalk verb patch cube sell west penalty fish park worry tribe tourist chalk verb patch cube sell west penalty fish park worry tribe tourist",
                isVisible = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}