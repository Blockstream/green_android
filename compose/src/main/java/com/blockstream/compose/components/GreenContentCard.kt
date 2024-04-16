package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.compose.R
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium


@Composable
fun GreenContentCard(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    painter: Painter? = null,
    onClick: () -> Unit = {}
) {

    GreenCard(onClick = onClick, modifier = modifier) {

        GreenRow(padding = 0, verticalAlignment = Alignment.CenterVertically) {
            GreenColumn(padding = 0, space = 8, modifier = Modifier.weight(1f)) {
                GreenRow(padding = 0, space = 8) {
                    painter?.also {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(text = title, style = titleSmall)
                }

                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        color = whiteMedium
                    )
                }
            }

            GreenArrow()
        }

    }
}

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