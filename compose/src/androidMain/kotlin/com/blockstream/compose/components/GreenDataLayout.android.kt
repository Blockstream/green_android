package com.blockstream.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_error
import com.blockstream.compose.theme.GreenChromePreview
import org.jetbrains.compose.resources.stringResource


@Preview
@Composable
fun GreenDataLayoutPreview() {
    GreenChromePreview {
        GreenColumn {
            GreenDataLayout() {
                Text(text = "Test")
            }

            GreenDataLayout(title = stringResource(Res.string.id_amount)) {
                Text(text = "Test")
            }

            var error by remember {
                mutableStateOf<String?>("Error")
            }

            GreenDataLayout(title = stringResource(Res.string.id_error), helperText = error, onClick = {
                if (error == null) {
                    error = "This is an error"
                } else {
                    error = null
                }
            }) {
                Text(text = "Test")
            }
        }
    }
}