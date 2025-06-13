package com.blockstream.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * A composable that maintains consistent height across components by using a placeholder layout.
 * The placeholder establishes the maximum height needed, while the actual content is overlaid on top.
 *
 * @param modifier Modifier to be applied to the Box
 * @param placeholder Composable that defines the maximum height layout (rendered invisibly)
 * @param content Actual content to be displayed
 */
@Composable
fun ConsistentHeightBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    placeholder: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = contentAlignment) {
        Box(modifier = Modifier.alpha(0f)) {
            placeholder()
        }

        content()
    }
}