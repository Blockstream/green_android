package com.blockstream.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun GreenSwitch(
    modifier: Modifier = Modifier,
    title: String,
    caption: String? = null,
    checked: Boolean,
    painter: Painter? = null,
    onCheckedChange: ((Boolean) -> Unit) = {},
) {
    GreenRow(
        space = 16,
        padding = 0,
        modifier = Modifier
            .clickable {
                onCheckedChange.invoke(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically

    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = Color.White,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}