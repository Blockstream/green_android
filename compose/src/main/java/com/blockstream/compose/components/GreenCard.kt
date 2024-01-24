package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.GreenThemePreview

@Composable
fun GreenCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        shape = shape,
        elevation = elevation,
        colors = colors,
        border = border
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
@Preview()
fun GreenCardPreview() {
    GreenThemePreview {
        GreenColumn(
            Modifier
                .padding(24.dp)
                .padding(24.dp)
        ) {
            GreenCard {
                Text(text = "This is a GreenCard")
            }

            GreenCard {
                Text(
                    text = "This is a GreenCard", modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}