package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_bitcoin_price_chart
import blockstream_green.common.generated.resources.id_buy_bitcoin
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_connects_directly
import blockstream_green.common.generated.resources.id_goes_through_tor_or_your_proxy
import blockstream_green.common.generated.resources.id_i_understand_tor_proxy_coverage
import blockstream_green.common.generated.resources.id_lightning
import blockstream_green.common.generated.resources.id_push_notifications
import blockstream_green.common.generated.resources.id_swaps
import blockstream_green.common.generated.resources.id_usage_analytics_if_enabled
import blockstream_green.common.generated.resources.id_wallet_sync_balances_and_transactions
import blockstream_green.common.generated.resources.id_whats_protected
import blockstream_green.common.generated.resources.id_your_wallet_connection_goes_through_tor
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Warning
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkPrivacyBottomSheet(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    GreenBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        bottomPadding = 16.dp,
        onDismissRequest = onDismissRequest
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.id_whats_protected),
                    style = titleMedium,
                    color = whiteHigh
                )

                Text(
                    text = stringResource(Res.string.id_your_wallet_connection_goes_through_tor),
                    style = bodyMedium,
                    color = whiteMedium
                )
            }

            CoverageSection(
                title = Res.string.id_goes_through_tor_or_your_proxy,
                icon = PhosphorIcons.Regular.Check,
                items = listOf(
                    Res.string.id_wallet_sync_balances_and_transactions,
                    Res.string.id_usage_analytics_if_enabled
                )
            )

            CoverageSection(
                title = Res.string.id_connects_directly,
                icon = PhosphorIcons.Regular.Warning,
                items = listOf(
                    Res.string.id_lightning,
                    Res.string.id_swaps,
                    Res.string.id_push_notifications,
                    Res.string.id_buy_bitcoin,
                    Res.string.id_bitcoin_price_chart
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GreenButton(
                    text = stringResource(Res.string.id_i_understand_tor_proxy_coverage),
                    size = GreenButtonSize.LARGE,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "network_privacy_confirm",
                    onClick = onConfirm
                )

                GreenButton(
                    text = stringResource(Res.string.id_cancel),
                    type = GreenButtonType.TEXT,
                    size = GreenButtonSize.LARGE,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "network_privacy_cancel",
                    onClick = onDismissRequest
                )
            }
        }
    }
}

@Composable
private fun CoverageSection(
    title: StringResource,
    icon: ImageVector,
    items: List<StringResource>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(title),
            style = labelLarge,
            color = whiteHigh,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        items.forEach { item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = whiteMedium,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(item),
                    style = bodyMedium,
                    color = whiteMedium
                )
            }
        }
    }
}
