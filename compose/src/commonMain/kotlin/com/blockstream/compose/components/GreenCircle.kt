package com.blockstream.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.md_theme_primary
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GreenCircle(size: Int, color: Color = md_theme_primary) {
    Canvas(modifier = Modifier.size(size.dp), onDraw = {
        val sizePx = size.dp.toPx()
        drawCircle(
            color = color,
            radius = sizePx / 2f
        )
    })
}

@Composable
@Preview
fun GreenCirclePreview() {
    GreenThemePreview {
        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            GreenCircle(size = 1)
            GreenCircle(size = 8)
            GreenCircle(size = 16)
            GreenCircle(size = 24)
        }
    }
}