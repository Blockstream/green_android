package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_asset
import com.blockstream.common.extensions.previewAssetBalance
import com.blockstream.compose.theme.GreenChromePreview
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun GreenAssetPreview() {
    GreenChromePreview {
        GreenColumn {
            GreenAsset(
                assetBalance = previewAssetBalance(),
                title = stringResource(Res.string.id_asset)
            )

            GreenAsset(assetBalance = previewAssetBalance(), withEditIcon = true)

            GreenAsset(withEditIcon = true, onClick = {})
        }
    }
}