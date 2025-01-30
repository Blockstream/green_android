package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_lightning_account_shortcut
import blockstream_green.common.generated.resources.id_ok_i_understand
import blockstream_green.common.generated.resources.id_with_this_shortcut_youll
import blockstream_green.common.generated.resources.lightning_shortcut
import com.blockstream.common.Urls
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.HandleSideEffectDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun LightningShortcutDialog(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismissRequest()
        }
    ) {

        HandleSideEffectDialog(viewModel, onDismiss = {
            onDismissRequest()
        })

        GreenCard(modifier = Modifier.fillMaxWidth()) {

            GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(Res.drawable.lightning_shortcut),
                    contentDescription = null
                )

                Text(
                    text = stringResource(Res.string.id_lightning_account_shortcut),
                    style = titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(Res.string.id_with_this_shortcut_youll),
                    style = bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                GreenColumn(
                    padding = 0, space = 24, modifier = Modifier
                        .fillMaxWidth()
                ) {

                    GreenButton(
                        text = stringResource(Res.string.id_learn_more,),
                        type = GreenButtonType.TEXT,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(Events.OpenBrowser(Urls.HELP_LIGHTNING_SHORTCUT))
                        onDismissRequest()
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_ok_i_understand),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onDismissRequest()
                    }
                }
            }
        }
    }
}
