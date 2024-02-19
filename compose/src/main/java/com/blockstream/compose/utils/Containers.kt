package com.blockstream.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext


@Composable
fun CopyContainer(
    modifier: Modifier = Modifier,
    value: String,
    withSelection: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.clickable {
        copyToClipboard(context = context, "Green", content = value)
    }) {
        if (withSelection) {
            SelectionContainer {
                content()
            }
        } else {
            content()
        }
    }
}