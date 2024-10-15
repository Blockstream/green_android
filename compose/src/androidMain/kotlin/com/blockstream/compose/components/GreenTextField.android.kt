package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_address
import blockstream_green.common.generated.resources.id_private_key
import com.blockstream.compose.theme.GreenChromePreview
import org.jetbrains.compose.resources.stringResource


@Preview
@Composable
fun GreenTextFieldPreview() {
    GreenChromePreview {
        GreenColumn {
            GreenTextField(title = stringResource(Res.string.id_address), "123", {})
            GreenTextField(title = stringResource(Res.string.id_private_key), "", {})
            GreenTextField(
                stringResource(Res.string.id_private_key),
                "",
                {},
                error = "id_insufficient_funds"
            )
            GreenTextField("With QR", "", {}, onQrClick = {

            })
        }
    }
}