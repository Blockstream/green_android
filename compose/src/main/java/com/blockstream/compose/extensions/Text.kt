package com.blockstream.compose.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.blockstream.compose.theme.md_theme_primary
import com.blockstream.compose.theme.whiteHigh

fun colorText(text: String, coloredTexts: List<String>, color: Color = md_theme_primary): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = whiteHigh)){
            append(text)
        }

        coloredTexts.map { it.lowercase() }.onEachIndexed { index, coloredText ->
            val start = text.lowercase().indexOf(coloredText)
            if (start != -1) {
                addStyle(SpanStyle(color = color), start, start + coloredText.length)

                addStringAnnotation(
                    tag = "Index",
                    annotation = index.toString(),
                    start = start,
                    end = start + coloredText.length
                )
            }
        }
    }
}