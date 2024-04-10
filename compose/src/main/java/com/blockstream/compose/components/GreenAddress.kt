package com.blockstream.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.extensions.colorTextEdges
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.monospaceFont
import com.blockstream.compose.utils.CopyContainer


@Composable
fun GreenAddress(modifier: Modifier = Modifier, address: String) {
    val schemes = listOf("bitcoin", "liquidnetwork", "liquidtestnet", "lightning")

    val text = if (!schemes.any { address.startsWith(it) }){
        address.chunked(4).joinToString(" ").let {
            colorTextEdges(text = it, numberOfSections = 2)
        }
    } else {
        AnnotatedString(address)
    }

    CopyContainer(value = address, withSelection = false) {
        Text(text = text, fontFamily = monospaceFont, modifier = modifier)
    }
}

@Preview
@Composable
fun GreenAddressPreview() {
    GreenThemePreview {
        GreenColumn {
            GreenAddress(address = "sml")
            GreenAddress(address = "1Dyf7wmQfcMwsvYct54sTKpmLzHwGyurPk")
            GreenAddress(address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu")
            GreenAddress(address = "bc1q7s60uyszam87cjxpdy6u0kr24cqjm6ydnkzkhgsh9htw2u6k3k5sl37hqp")
            GreenAddress(address = "VJLJpBiGryNd6ExVvow3ek9CFCuqhSjCoHA7PBhdFYNyruGe9UMPTWSjDCXZfsPvaw7ccHhHPuahXQ12")
            GreenAddress(address = "bitcoin:bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu?amount=123")
        }
    }
}