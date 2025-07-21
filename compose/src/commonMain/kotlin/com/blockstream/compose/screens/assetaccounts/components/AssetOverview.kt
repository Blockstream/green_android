package com.blockstream.compose.screens.assetaccounts.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.headlineMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.ui.components.GreenColumn

@Composable
internal fun AssetOverview(
    asset: EnrichedAsset?,
    totalBalance: String,
    totalBalanceFiat: String?,
    session: GdkSession?,
    modifier: Modifier = Modifier
) {
    GreenColumn(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        space = 0,
        padding = 0,
    ) {
        Image(
            painter = asset?.assetId.assetIcon(session = session),
            contentDescription = asset?.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 8.dp)
        )

        Text(
            text = totalBalance,
            style = headlineMedium,
            color = whiteHigh,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            fontWeight = FontWeight.W500
        )

        totalBalanceFiat?.let { fiat ->
            Text(
                text = fiat,
                style = bodyMedium,
                color = whiteMedium,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}