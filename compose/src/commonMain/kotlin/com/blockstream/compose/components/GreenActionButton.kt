package com.blockstream.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_swap
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Swap
import com.blockstream.compose.GreenPreview
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun RowScope.GreenActionButton(
    text: StringResource,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    GreenCard(
        modifier = modifier
            .weight(1f),
        enabled = enabled,
        padding = 0,
        onClick = onClick
    ) {
        GreenColumn(
            padding = 0,
            space = 8,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Text(text = stringResource(text), textAlign = TextAlign.Center)
        }
    }
}

@Composable
@Preview
fun GreenActionButtonPreview() {
    GreenPreview {
        Row {
            GreenActionButton(
                text = Res.string.id_swap,
                icon = PhosphorIcons.Regular.Swap,
                onClick = { }
            )
        }
    }
}
