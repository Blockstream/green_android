package com.blockstream.compose.screens.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.lightning
import com.blockstream.compose.theme.md_theme_primary
import com.blockstream.compose.theme.whiteHigh

internal fun isHumanReadableLightningInput(input: String): Boolean {
    val trimmed = input.trim()
    return trimmed.startsWith("₿") || ('@' in trimmed && !trimmed.contains(':'))
}

@Composable
internal fun ChunkedInvoice(invoice: String, isLightning: Boolean = false) {
    if (isHumanReadableLightningInput(invoice)) {
        Text(
            text = invoice,
            style = bodyMedium,
            color = whiteHigh,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    val highlightColor = if (isLightning) lightning else md_theme_primary

    if (invoice.length < 32) {
        Text(
            text = invoice.chunked(4).joinToString(" "),
            style = bodyMedium.copy(fontFamily = MonospaceFont()),
            color = whiteHigh,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    val head = invoice.take(16).chunked(4)
    val tail = invoice.takeLast(16).chunked(4)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(220.dp),
    ) {
        ChunkRow(chunks = head, highlightIndices = setOf(0, 1), highlightColor = highlightColor)

        Text(
            text = "...",
            style = bodyMedium.copy(fontFamily = MonospaceFont()),
            color = whiteHigh,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        ChunkRow(chunks = tail, highlightIndices = setOf(2, 3), highlightColor = highlightColor)
    }
}

@Composable
private fun ChunkRow(
    chunks: List<String>,
    highlightIndices: Set<Int>,
    highlightColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        chunks.forEachIndexed { index, chunk ->
            Text(
                text = chunk,
                style = bodyMedium.copy(fontFamily = MonospaceFont()),
                color = if (index in highlightIndices) highlightColor else whiteHigh,
            )
        }
    }
}
