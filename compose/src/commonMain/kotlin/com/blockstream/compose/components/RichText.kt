package com.blockstream.compose.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign

@Composable
fun RichText(
    text: String,
    spans: List<RichSpan>,
    modifier: Modifier = Modifier,
    paragraph: ParagraphStyle = ParagraphStyle(textAlign = TextAlign.Center),
    defaultStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val annotated = remember(text, spans) {
        buildAnnotatedString {
            append(text)

            spans.forEach { span ->
                val regex = Regex(Regex.escape(span.text))

                regex.findAll(text).forEach { m ->
                    val start = m.range.first
                    val end   = m.range.last + 1

                    addStyle(span.style, start, end)

                    span.onClick?.let { handler ->
                        addLink(
                            LinkAnnotation.Clickable(
                                tag = "RichTextLink",
                                styles = TextLinkStyles(style = span.style),
                                linkInteractionListener = { handler() }
                            ),
                            start = start,
                            end = end
                        )
                    }
                }
            }
        }
    }

    BasicText(
        text = annotated,
        modifier = modifier,
        style = defaultStyle.merge(
            TextStyle(textAlign = paragraph.textAlign)
        )
    )
}

data class RichSpan(
    val text: String,
    val style: SpanStyle = SpanStyle(),
    val onClick: (() -> Unit)? = null,
)