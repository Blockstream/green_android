package com.blockstream.compose.screens.receive.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_lightning_ready
import blockstream_green.common.generated.resources.lightning
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.lightning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LightningReadyBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(1.dp, lightning, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.lightning),
            contentDescription = null,
            tint = lightning,
            modifier = Modifier.size(20.dp).padding(end = 4.dp)
        )
        Text(
            text = stringResource(Res.string.id_lightning_ready),
            style = bodySmall,
            color = lightning
        )
    }
}
