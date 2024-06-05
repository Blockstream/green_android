package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.extensions.previewAssetBalance
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh

@Preview
@Composable
fun GreenAssetPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenAsset(
                assetBalance = previewAssetBalance(),
                title = stringResource(id = R.string.id_asset)
            )

            GreenAsset(assetBalance = previewAssetBalance(), withEditIcon = true)

            GreenAsset(withEditIcon = true, onClick = {})
        }
    }
}