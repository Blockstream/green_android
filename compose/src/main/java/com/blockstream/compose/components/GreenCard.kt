package com.blockstream.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.GreenTheme

@Composable
fun GreenCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = shape,
        elevation = elevation,
        colors = colors
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(modifier),
            content = content
        )
    }
}

@Composable
@Preview()
fun GreenCardPreview() {
    GreenTheme {
        Box(
            Modifier
                .padding(24.dp)
                .background(Color.Yellow)
                .padding(24.dp)
        ) {
            GreenCard {
                Text(text = "This is a GreenCard")
            }
        }
    }
}