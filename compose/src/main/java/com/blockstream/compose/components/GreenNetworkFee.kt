package com.blockstream.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.data.FeePriority
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.roundBackground
import com.blockstream.compose.utils.stringResourceId

@Composable
fun GreenNetworkFee(
    feePriority: FeePriority,
    title: String? = null,
    withEditIcon: Boolean = true,
    onClick: ((onEditClick: Boolean) -> Unit)? = null
) {
    GreenDataLayout(
        title = title ?: stringResource(R.string.id_network_fee),
        error = feePriority.error,
        withPadding = false,
        onClick = if (onClick != null) {
            {
                onClick(false)
            }
        } else null
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .padding(vertical = 8.dp)
                .heightIn(min = 48.dp) // If Edit Icon is hidden
        ) {
            Column {
                GreenRow(
                    padding = 0,
                    space = 4,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResourceId(feePriority.title), style = titleSmall)
                    AnimatedNullableVisibility(value = feePriority.expectedConfirmationTime) {
                        Text(
                            text = stringResourceId(it),
                            style = bodyMedium,
                            color = whiteMedium,
                            modifier = Modifier.roundBackground(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(feePriority.feeRate ?: "", style = bodySmall, color = whiteMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = if (withEditIcon && onClick != null) 0.dp else 16.dp)
            ) {

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = feePriority.fee ?: "",
                        color = whiteMedium,
                        style = labelLarge
                    )
                    Text(
                        text = feePriority.feeFiat ?: "",
                        color = whiteMedium,
                        style = bodyMedium
                    )
                }

                if(withEditIcon){
                    IconButton(onClick = {
                        onClick?.invoke(true)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.pencil_simple_line),
                            contentDescription = "Edit"
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun GreenNetworkFeePreview() {
    GreenThemePreview {
        GreenColumn {
            GreenNetworkFee(FeePriority.High(), onClick = {})
            GreenNetworkFee(FeePriority.Medium(fee = "1 BTC", feeFiat = "12,00 USD", feeRate = "1 sats/vbyte"), onClick = {})
            GreenNetworkFee(FeePriority.Low(error = "id_insufficient_funds"),onClick = {})
            GreenNetworkFee(FeePriority.High(fee = "1 BTC", feeFiat = "12,00 USD", feeRate = "1 sats/vbyte"), withEditIcon = false, onClick = {})
        }
    }
}