package com.blockstream.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.blockstream.compose.extensions.colorTextEdges
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.utils.CopyContainer


@Composable
fun GreenAddress(
    modifier: Modifier = Modifier,
    address: String,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    onCopyClick: ((String) -> Unit)? = null
) {
    val schemes = listOf("bitcoin", "liquidnetwork", "liquidtestnet", "lightning")

    val text = if (!schemes.any { address.startsWith(it) }) {
        address.chunked(4).joinToString(" ").let {
            colorTextEdges(text = it, numberOfSections = 2)
        }
    } else {
        AnnotatedString(address)
    }

    val content = @Composable {
        Text(
            text = text,
            fontFamily = MonospaceFont(),
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (onCopyClick == null) {
        CopyContainer(value = address, withSelection = false) {
            content()
        }
    } else {
        Box(modifier = Modifier.clickable {
            onCopyClick(address)
        }) {
            content()
        }
    }
}
