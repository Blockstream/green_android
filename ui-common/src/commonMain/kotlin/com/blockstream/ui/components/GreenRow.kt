package com.blockstream.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
inline fun GreenRow(
    modifier: Modifier = Modifier,
    space: Int = 16,
    padding: Int = 16,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(space.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.padding(padding.dp).then(modifier),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}