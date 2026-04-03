package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_i_understand
import blockstream_green.common.generated.resources.id_lightning_beta_disclaimer
import blockstream_green.common.generated.resources.id_lightning_is_in_beta
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.LearnMoreButton
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.textMedium
import com.blockstream.compose.theme.titleLarge
import org.jetbrains.compose.resources.stringResource

@Composable
fun LightningBetaDialog(
    onUnderstand: () -> Unit,
    onLearnMore: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GreenCard(modifier = Modifier.fillMaxWidth()) {
            GreenColumn(
                padding = 0,
                space = 16,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.id_lightning_is_in_beta),
                    style = titleLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(Res.string.id_lightning_beta_disclaimer),
                    style = bodyMedium,
                    color = textMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                GreenButton(
                    text = stringResource(Res.string.id_i_understand),
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    onUnderstand()
                }

                LearnMoreButton(onClick = onLearnMore)
            }
        }
    }
}
