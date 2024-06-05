package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_right
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_outline
import com.blockstream.compose.theme.whiteHigh
import org.jetbrains.compose.resources.painterResource

@Composable
fun GreenArrow(modifier: Modifier = Modifier, enabled: Boolean = true) {

    Card(
        Modifier
            .then(modifier)
            .size(40.dp),
        colors = CardDefaults.cardColors(
            contentColor = whiteHigh,
            containerColor = if(enabled) green else md_theme_outline
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Image(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(Res.drawable.arrow_right),
                contentDescription = ""
            )
        }
    }
}
