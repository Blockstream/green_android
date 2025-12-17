package com.blockstream.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_change_speed
import com.blockstream.compose.extensions.title
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.stringResourceFromIdOrNull
import com.blockstream.data.data.FeePriority
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NetworkFeeLine(
    feePriority: FeePriority,
    onClick: ((onEditClick: Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton({
            onClick?.invoke(false)
        }, content = {
            Text(stringResource(Res.string.id_change_speed), style = bodyMedium, color = green)
        })

        AnimatedNullableVisibility(value = feePriority.expectedConfirmationTime) {
            Text(text = "($it)", modifier = Modifier.offset(x = (-6).dp), style = bodyMedium, color = whiteLow)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (feePriority.error != null) {
            Text(text = stringResourceFromIdOrNull(feePriority.error) ?: "", style = bodyMedium, color = red)
        } else {
            Text(text = feePriority.feeRate ?: stringResource(feePriority.title), style = bodyMedium, color = whiteLow)
        }
    }
}

@Preview
@Composable
fun NetworkFeeLinePreview() {
    GreenChromePreview {
        Column {
            NetworkFeeLine(FeePriority.High(), onClick = {})
            NetworkFeeLine(
                FeePriority.Medium(
                    fee = "1 BTC",
                    feeFiat = "12,00 USD",
                    feeRate = "1 sats/vbyte",
                    expectedConfirmationTime = "~2 hours"
                ), onClick = {})
            NetworkFeeLine(FeePriority.Low(error = "id_insufficient_funds"), onClick = {})
            NetworkFeeLine(
                FeePriority.High(
                    fee = "1 BTC",
                    feeFiat = "12,00 USD",
                    feeRate = "1 sats/vbyte",
                    expectedConfirmationTime = "~2 hours"
                ), onClick = {})
        }
    }
}