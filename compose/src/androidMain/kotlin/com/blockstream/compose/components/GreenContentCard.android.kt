package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.bitcoin
import blockstream_green.common.generated.resources.bitcoin_testnet
import blockstream_green.common.generated.resources.id_import_a_wallet_created_with
import blockstream_green.common.generated.resources.key_singlesig
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.GreenChromePreview
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
@Preview
fun GreenContentCardPreview() {
    GreenChromePreview {
        GreenColumn() {
            GreenContentCard(
                title = "Bitcoin",
                message = stringResource(Res.string.id_import_a_wallet_created_with),
                painter = painterResource(Res.drawable.key_singlesig)
            )

            GreenContentCard(
                title = "Bitcoin",
                message = stringResource(Res.string.id_import_a_wallet_created_with),
                painter = painterResource(Res.drawable.bitcoin)
            )

            GreenContentCard(
                title = "Testnet",
                message = "",
                painter = painterResource(Res.drawable.bitcoin_testnet)
            )
        }
    }
}