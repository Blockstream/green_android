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
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_network_fee
import blockstream_green.common.generated.resources.pencil_simple_line
import com.blockstream.data.data.FeePriority
import com.blockstream.compose.extensions.title
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.roundBackground
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenNetworkFee(
    feePriority: FeePriority,
    title: String? = null,
    withEditIcon: Boolean = true,
    onClick: ((onEditClick: Boolean) -> Unit)? = null
) {
    GreenDataLayout(
        title = title ?: stringResource(Res.string.id_network_fee),
        helperText = feePriority.error,
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
                    Text(stringResource(feePriority.title), style = titleSmall)
                    AnimatedNullableVisibility(value = feePriority.expectedConfirmationTime) {
                        Text(
                            text = it,
                            style = bodyMedium,
                            color = whiteMedium,
                            modifier = Modifier.roundBackground(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(feePriority.feeRate ?: "", style = bodyMedium, color = whiteMedium)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
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

                if (withEditIcon) {
                    IconButton(onClick = {
                        onClick?.invoke(true)
                    }) {
                        Icon(
                            painter = painterResource(Res.drawable.pencil_simple_line),
                            contentDescription = "Edit"
                        )
                    }
                }
            }
        }
    }
}