package com.blockstream.compose.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GreenDataLayout(
    modifier: Modifier = Modifier,
    title: String? = null,
    helperText: String? = null,
    helperContainerColor: Color? = null,
    withPadding: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        title?.also {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        GreenCard(
            padding = (if (withPadding) 16 else 0),
            onClick = onClick,
            helperText = helperText,
            enabled = enabled,
            helperContainerColor = helperContainerColor,
            content = content
        )
    }
}