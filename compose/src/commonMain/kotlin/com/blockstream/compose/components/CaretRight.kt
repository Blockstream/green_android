package com.blockstream.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.compose.theme.GreenChromePreview
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CaretRight(modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = Modifier.then(modifier).size(40.dp)
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Center),
            imageVector = PhosphorIcons.Regular.CaretRight,
            contentDescription = "Arrow",
            tint = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

@Composable
@Preview
fun CaretRightPreview() {
    GreenChromePreview {
        GreenColumn {
            CaretRight()
            CaretRight(enabled = false)
        }
    }
}