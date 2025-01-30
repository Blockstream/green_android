package com.blockstream.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
@NonRestartableComposable
fun GreenSpacer(space: Int = 16) {
    Spacer(
        modifier = Modifier
            .size(space.dp)
    )
}