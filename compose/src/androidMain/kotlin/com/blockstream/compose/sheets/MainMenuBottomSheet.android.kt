package com.blockstream.compose.sheets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn


@Composable
@Preview
fun MainMenuBottomSheetPreview() {
    GreenAndroidPreview {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            var environment by remember { mutableStateOf("-") }
            Text("MenuBottomSheetPreview env: $environment")

            if (showBottomSheet) {
                MainMenuBottomSheetView(
                    onSelect = { menuEntry ->

                    },
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}