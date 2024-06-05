package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview


@Preview
@Composable
fun GreenTextFieldPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenTextField(title = stringResource(R.string.id_address), "123", {})
            GreenTextField(title = stringResource(R.string.id_private_key), "", {})
            GreenTextField(
                stringResource(R.string.id_private_key),
                "",
                {},
                error = "id_insufficient_funds"
            )
            GreenTextField("With QR", "", {}, onQrClick = {

            })
        }
    }
}