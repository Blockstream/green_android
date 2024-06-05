package com.blockstream.compose.components

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
    maxLines: Int = Int.MAX_VALUE
) {
    val schemes = listOf("bitcoin", "liquidnetwork", "liquidtestnet", "lightning")

    val text = if (!schemes.any { address.startsWith(it) }) {
        address.chunked(4).joinToString(" ").let {
            colorTextEdges(text = it, numberOfSections = 2)
        }
    } else {
        AnnotatedString(address)
    }

    CopyContainer(value = address, withSelection = false) {
        Text(
            text = text,
            fontFamily = MonospaceFont(),
            modifier = modifier,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
