package com.blockstream.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview


@Preview
@Composable
fun GreenDataLayoutPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenDataLayout() {
                Text(text = "Test")
            }

            GreenDataLayout(title = stringResource(R.string.id_amount)) {
                Text(text = "Test")
            }

            var error by remember {
                mutableStateOf<String?>("Error")
            }

            GreenDataLayout(title = stringResource(R.string.id_error), helperText = error, onClick = {
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