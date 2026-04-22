package com.blockstream.compose.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_done
import blockstream_green.common.generated.resources.id_transaction_successful
import blockstream_green.common.generated.resources.id_you_transferred_s
import blockstream_green.common.generated.resources.success_tick
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.md_theme_dialogSurface
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TransactionSuccessDialog(
    amount: String,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GreenCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            padding = 0,
            colors = CardDefaults.outlinedCardColors(containerColor = md_theme_dialogSurface),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = whiteMedium,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(top = 56.dp, bottom = 32.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.success_tick),
                        contentDescription = null,
                        modifier = Modifier.size(124.dp),
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 32.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.id_transaction_successful),
                            style = titleSmall,
                            color = whiteHigh,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(Res.string.id_you_transferred_s, amount),
                            style = bodyMedium,
                            color = whiteMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_done),
                        size = GreenButtonSize.BIG,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp),
                        onClick = onDismissRequest,
                    )
                }
            }
        }
    }
}
