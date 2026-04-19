package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_network_fee
import blockstream_green.common.generated.resources.id_total_fees
import blockstream_green.common.generated.resources.id_total_spent
import blockstream_green.common.generated.resources.id_total_to_receive
import blockstream_green.common.generated.resources.info
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.data.transaction.TransactionConfirmation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun ReviewDataRow(
    title: String,
    subtitle: String? = null,
    value: String,
    valueSecondary: String? = null,
    isLarge: Boolean = false,
    onTitleClick: (() -> Unit)? = null
) {
    Row {
        Column {
            Row {
                Text(
                    text = title, color = whiteMedium, style = if (isLarge) titleSmall else labelLarge
                )

                if (onTitleClick != null) {
                    IconButton(
                        onClick = {
                            onTitleClick()
                        }, modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.info),
                            contentDescription = null,
                            tint = whiteMedium,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = whiteMedium,
                    style = bodySmall
                )
            }
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
            Text(text = value, color = whiteHigh, style = if (isLarge) titleSmall else labelLarge)

            if (valueSecondary != null) {
                Text(
                    text = valueSecondary,
                    color = whiteMedium,
                    style = if (isLarge) labelLarge else bodyMedium
                )
            }
        }
    }
}

@Composable
fun TransactionConfirmationSummary(
    confirmation: TransactionConfirmation,
    onTotalFeesClick: (() -> Unit)? = null
) {
    if (confirmation.isLiquidToLightningSwap || confirmation.isSwap) {
        ReviewDataRow(
            title = stringResource(Res.string.id_total_fees),
            onTitleClick = onTotalFeesClick,
            value = confirmation.totalFees ?: ""
        )

        if (confirmation.isSwap) {
            ReviewDataRow(
                title = stringResource(Res.string.id_total_spent),
                value = confirmation.total ?: ""
            )
        }
    } else {
        ReviewDataRow(
            title = stringResource(Res.string.id_network_fee),
            subtitle = confirmation.feeRate,
            value = confirmation.fee ?: "",
            valueSecondary = confirmation.feeFiat
        )
    }

    if (confirmation.isSwap || confirmation.total != null || confirmation.totalFiat != null) {
        HorizontalDivider()
    }

    if (confirmation.isSwap) {
        ReviewDataRow(
            title = stringResource(Res.string.id_total_to_receive),
            value = confirmation.recipientReceives ?: "",
            valueSecondary = confirmation.recipientReceivesFiat,
            isLarge = true
        )
    } else {
        val total = confirmation.total
        val totalFiat = confirmation.totalFiat
        if (total != null) {
            ReviewDataRow(
                title = stringResource(Res.string.id_total_spent),
                value = total,
                valueSecondary = totalFiat,
                isLarge = false
            )
        } else if (totalFiat != null) {
            ReviewDataRow(
                title = stringResource(Res.string.id_total_spent),
                value = totalFiat,
                isLarge = false
            )
        }
    }
}
