package com.blockstream.compose.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun GreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GreenColors,
        shapes = GreenShapes,
        typography = GreenTypography()
    ){
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}