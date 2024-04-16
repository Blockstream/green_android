package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.blockstream.common.Urls
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.HandleSideEffectDialog


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
                    painter = painterResource(id = R.drawable.lightning_shortcut),
                    contentDescription = null
                )

                Text(
                    text = stringResource(id = R.string.id_lightning_account_shortcut_activated),
                    style = titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(id = R.string.id_with_this_shortcut_you_ll_enjoy),
                    style = bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                GreenColumn(
                    padding = 0, space = 24, modifier = Modifier
                        .fillMaxWidth()
                ) {

                    GreenButton(
                        text = stringResource(
                            R.string.id_learn_more,
                        ),
                        type = GreenButtonType.TEXT,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(Events.OpenBrowser(Urls.HELP_LIGHTNING_SHORTCUT))
                        onDismissRequest()
                    }

                    GreenButton(
                        text = stringResource(id = R.string.id_ok_i_understand),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onDismissRequest()
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun LightningShortcutDialogPreview() {
    GreenThemePreview {
        LightningShortcutDialog(
            viewModel = SimpleGreenViewModelPreview()
        ) {

        }
    }
}