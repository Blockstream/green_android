package com.blockstream.compose.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.blockstream.compose.theme.md_theme_primary
import com.blockstream.compose.theme.whiteHigh
import kotlin.math.max
import kotlin.math.min

fun colorText(
    text: String,
    coloredTexts: List<String>,
    baseColor: Color = whiteHigh,
    color: Color = md_theme_primary
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = baseColor)){
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

fun colorTextEdges(text: String, numberOfChars: Int? = null, color: Color = md_theme_primary): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = whiteHigh)){
            append(text)
        }

        val leading = numberOfChars ?: min(text.indexOf(" "), text.length)
        val trailing = numberOfChars?.let { text.length - it } ?: max(0, text.lastIndexOf(" "))

        if (text.length > 0 && leading >= 0 && leading < text.length && trailing >= 0 && trailing < text.length) {

            val style = SpanStyle(color = color)
            addStyle(style, 0, leading)
            addStyle(style, trailing, text.length)
        }
    }
}