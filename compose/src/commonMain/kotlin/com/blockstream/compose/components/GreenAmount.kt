package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.blockstream.common.gdk.GdkSession
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleMedium

@Composable
fun GreenAmount(
    modifier: Modifier = Modifier,
    title: String? = null,
    amount: String,
    amountFiat: String? = null,
    assetId: String? = null,
    address: String? = null,
    session: GdkSession? = null,
    showIcon: Boolean = false
) {
    GreenDataLayout(title = title, modifier = modifier) {
        GreenColumn(padding = 0, space = 8) {
            address?.also { GreenAddress(address = it) }

            Box {
                if(showIcon) {
                    Image(
                        painter = assetId.assetIcon(session = session),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(32.dp)

                    )
                }

                Column(
                    horizontalAlignment = if (showIcon) Alignment.End else Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()

                ) {

                    SelectionContainer {
                        Text(text = amount, style = titleMedium)
                    }

                    amountFiat?.also { fiat ->
                        SelectionContainer {
                            Text(text = fiat, style = bodyLarge)
                        }
                    }
                }
            }
        }
    }
}