package com.blockstream.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.blockstream.compose.extensions.colorTextEdges
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.utils.CopyContainer
import com.blockstream.ui.utils.ifTrue

@Composable
fun GreenAddress(
    address: String,
    textAlign: TextAlign? = null,
    showCopyIcon: Boolean = false,
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
        Row {
            Text(
                modifier = Modifier.ifTrue(showCopyIcon) {
                    it.weight(1f) // Make it fill the available space, else copy icon will be out of visible area
                },
                text = text,
                fontFamily = MonospaceFont(),
                textAlign = textAlign,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )

            if (showCopyIcon) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Copy,
                    contentDescription = "Copy",
                    tint = whiteHigh,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(start = 8.dp)
                )
            }
        }
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
