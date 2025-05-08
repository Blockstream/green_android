package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_error
import com.blockstream.compose.GreenAndroidPreview
import com.blockstream.compose.theme.green
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.stringResource


@Preview
@Composable
fun GreenDataLayoutPreview() {
    GreenAndroidPreview {
        GreenColumn {
            GreenDataLayout(badge = "Badge") {
                Text(text = "Test")
            }

            GreenDataLayout(title = stringResource(Res.string.id_amount)) {
                Text(text = "Test")
            }

            GreenDataLayout(
                title = stringResource(Res.string.id_amount),
                border = BorderStroke(1.dp, green)
            ) {
                Text(text = "Test")
            }

            var error by remember {
                mutableStateOf<String?>("Error")
            }

            GreenDataLayout(title = stringResource(Res.string.id_error), helperText = error, onClick = {
                error = if (error == null) {
                    "This is an error"
                } else {
                    null
                }
            }) {
                Text(text = "Test")
            }
        }
    }
}