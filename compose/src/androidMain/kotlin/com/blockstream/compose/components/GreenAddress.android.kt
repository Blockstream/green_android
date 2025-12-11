package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.theme.GreenChromePreview

@Preview
@Composable
fun GreenAddressPreview() {
    GreenChromePreview {

        GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            GreenAddress(address = "bc1tinyaddresstestonly", showCopyIcon = true)
            GreenAddress(address = "bc1tinyaddresstestonly")
            GreenAddress(address = "1Dyf7wmQfcMwsvYct54sTKpmLzHwGyurPk", showCopyIcon = true)
            GreenAddress(address = "1Dyf7wmQfcMwsvYct54sTKpmLzHwGyurPk")
            GreenAddress(address = "bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu")
            GreenAddress(address = "bc1q7s60uyszam87cjxpdy6u0kr24cqjm6ydnkzkhgsh9htw2u6k3k5sl37hqp")
            GreenAddress(address = "VJLJpBiGryNd6ExVvow3ek9CFCuqhSjCoHA7PBhdFYNyruGe9UMPTWSjDCXZfsPvaw7ccHhHPuahXQ12", showCopyIcon = true)
            GreenAddress(address = "bitcoin:bc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu?amount=123", showCopyIcon = true)
            GreenAddress(
                address = "lightning:LNURLasdfasdfadfasdfadfasdfadabc1qaqtq80759n35gk6ftc57vh7du83nwvt5lgkznu?amount=123",
                maxLines = 1,
                showCopyIcon = true
            )
        }
    }
}