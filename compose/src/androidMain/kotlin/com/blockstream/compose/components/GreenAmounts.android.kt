package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.looks.AmountAssetLook
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.R
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.noRippleClickable


@Preview
@Composable
fun GreenAmountsPreview() {
    GreenThemePreview {
        GreenColumn {
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1.91080032",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "-1,500",
                        assetId = BTC_POLICY_ASSET,
                        "BTC"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "-0.768920",
                        assetId = BTC_POLICY_ASSET,
                        "BTC"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "2,420.1234",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    )
                )
            )
            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1,232,321,543,322,420.1234567", assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    )
                )
            )

            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1.91080032",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    ),
                    AmountAssetLook("-2.1234", assetId = BTC_POLICY_ASSET, "L-BTC", "5,1231.23 EUR")
                )
            )

            HorizontalDivider()
            GreenAmounts(
                amounts = listOf(
                    AmountAssetLook(
                        "1,232,321,543,322,420.1234567",
                        assetId = BTC_POLICY_ASSET,
                        "BTC",
                        "5,1231.23 EUR"
                    ),
                    AmountAssetLook(
                        "-2,123,363,543,322,420.1234567",
                        assetId = BTC_POLICY_ASSET,
                        "L-BTC"
                    )
                )
            )
            HorizontalDivider()
        }
    }
}