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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_outline
import com.blockstream.compose.theme.whiteHigh

@Composable
fun GreenArrow(modifier: Modifier = Modifier, enabled: Boolean = true) {

    Card(
        Modifier
            .size(40.dp)
            .then(modifier),
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
                painter = painterResource(id = R.drawable.arrow_right),
                contentDescription = ""
            )
        }
    }
}


@Composable
@Preview
fun GreenArrowPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenArrow()
            GreenArrow(enabled = false)
        }
    }
}