package com.blockstream.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview


@Composable
@Preview
fun GreenContentCardPreview() {
    GreenThemePreview {
        GreenColumn() {
            GreenContentCard(
                title = "Bitcoin",
                message = stringResource(id = R.string.id_import_a_wallet_created_with),
                painter = painterResource(
                    id = R.drawable.key_singlesig
                )
            )

            GreenContentCard(
                title = "Bitcoin",
                message = stringResource(id = R.string.id_import_a_wallet_created_with),
                painter = painterResource(
                    id = R.drawable.bitcoin
                )
            )

            GreenContentCard(
                title = "Testnet",
                message = "",
                painter = painterResource(
                    id = R.drawable.bitcoin_testnet
                )
            )
        }
    }
}