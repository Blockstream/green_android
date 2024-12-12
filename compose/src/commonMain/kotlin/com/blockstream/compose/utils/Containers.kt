package com.blockstream.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockstream.compose.managers.LocalPlatformManager


@Composable
fun CopyContainer(
    modifier: Modifier = Modifier,
    value: String,
    withSelection: Boolean = true,
    content: @Composable () -> Unit
) {
    val platformManager = LocalPlatformManager.current

    Box(modifier = modifier.clickable {
        platformManager.copyToClipboard(content = value)
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