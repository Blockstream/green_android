package com.blockstream.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.blockstream.designsystem.LocalAppContentColor
import com.blockstream.designsystem.LocalAppTextStyle

@Composable
fun ProvideAppTextStyle(value: TextStyle, content: @Composable () -> Unit) {
    val mergedStyle = LocalAppTextStyle.current.merge(value)
    CompositionLocalProvider(LocalAppTextStyle provides mergedStyle, content = content)
}

@Composable
fun ProvideAppContentColorTextStyle(contentColor: Color, textStyle: TextStyle, content: @Composable () -> Unit) {
    val mergedStyle = LocalAppTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalAppContentColor provides contentColor,
        LocalAppTextStyle provides mergedStyle,
        content = content
    )
}
