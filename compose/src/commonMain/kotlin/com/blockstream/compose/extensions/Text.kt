package com.blockstream.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.blockstream.common.utils.lastNthIndexOf
import com.blockstream.common.utils.nthIndexOf
import com.blockstream.compose.theme.md_theme_primary
import com.blockstream.compose.theme.textHigh
import kotlin.math.sqrt

@Composable
fun colorText(
    text: String,
    coloredTexts: List<String>,
    baseColor: Color = textHigh,
    color: Color = md_theme_primary
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = baseColor)) {
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

@Composable
fun colorTextEdges(text: String, numberOfSections: Int = 1, color: Color = md_theme_primary): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = textHigh)) {
            append(text)
        }

        val leading = text.nthIndexOf(" ", numberOfSections)
        val trailing = text.lastNthIndexOf(" ", numberOfSections)

        if (text.isNotEmpty() && leading >= 0 && leading < text.length && trailing >= 0 && trailing < text.length) {
            val style = SpanStyle(color = color)
            addStyle(style, 0, leading)
            addStyle(style, trailing, text.length)
        }
    }
}

//fun linkText(text: String,
//             links: List<Pair<String, String>>,
//             baseColor: Color = whiteHigh,
//             color: Color = md_theme_primary
//) : AnnotatedString {
//
//    return buildAnnotatedString {
//        withStyle(style = SpanStyle(color = baseColor)){
//            append(text)
//        }
//
//        links.map { it.first.lowercase() }.onEachIndexed { index, coloredText ->
//            val start = text.lowercase().indexOf(coloredText)
//            if (start != -1) {
//                addStyle(SpanStyle(color = color), start, start + coloredText.length)
//
//                addStringAnnotation(
//                    tag = "URL",
//                    annotation = links[index].second,
//                    start = start,
//                    end = start + coloredText.length
//                )
//            }
//        }
//    }
//}

fun Modifier.drawDiagonalLabel(
    text: String,
    color: Color,
    style: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    ),
    labelTextRatio: Float = 7f
) = composed(
    factory = {

        val textMeasurer = rememberTextMeasurer()
        val textLayoutResult: TextLayoutResult = remember {
            textMeasurer.measure(text = AnnotatedString(text), style = style)
        }


        Modifier
            .clipToBounds()
            .drawWithContent {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val textSize = textLayoutResult.size
                val textWidth = textSize.width
                val textHeight = textSize.height

                val rectWidth = textWidth * labelTextRatio
                val rectHeight = textHeight * 1.1f

                val rect = Rect(
                    offset = Offset(canvasWidth - rectWidth, 0f),
                    size = Size(rectWidth, rectHeight)
                )

                val sqrt = sqrt(rectWidth / 2f)
                val translatePos = sqrt * sqrt

                drawContent()
                withTransform(
                    {
                        rotate(
                            degrees = 45f,
                            pivot = Offset(
                                canvasWidth - rectWidth / 2,
                                translatePos
                            )
                        )
                    }
                ) {
                    drawRect(
                        color = color,
                        topLeft = rect.topLeft,
                        size = rect.size
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        style = style,
                        topLeft = Offset(
                            rect.left + (rectWidth - textWidth) / 2f,
                            rect.top + (rect.bottom - textHeight) / 2f
                        )
                    )
                }

            }
    }
)