package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_archived
import blockstream_green.common.generated.resources.id_continue
import blockstream_green.common.generated.resources.id_see_archived_accounts_s
import blockstream_green.common.generated.resources.id_you_can_still_receive_funds_but
import com.blockstream.common.models.archived.ArchivedAccountsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.Rive
import com.blockstream.compose.components.RiveAnimation
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.HandleSideEffectDialog
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.stringResource


@Composable
fun ArchivedAccountsDialog(
    viewModel: ArchivedAccountsViewModelAbstract,
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
            Column {
                if (!LocalInspectionMode.current) {
                    Rive(RiveAnimation.ACCOUNT_ARCHIVED)
                }

                GreenColumn(padding = 0, space = 8, modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        text = stringResource(Res.string.id_account_archived),
                        style = titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(Res.string.id_you_can_still_receive_funds_but),
                        style = bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                GreenColumn(
                    padding = 0, space = 8, modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    GreenButton(
                        text = stringResource(Res.string.id_continue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onDismissRequest()
                    }

                    val archivedAccounts by viewModel.archivedAccounts.collectAsState()

                    GreenButton(
                        text = stringResource(
                            Res.string.id_see_archived_accounts_s,
                            archivedAccounts.data()?.size?.toString() ?: "-"
                        ),
                        type = GreenButtonType.OUTLINE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(NavigateDestinations.ArchivedAccounts(greenWallet = viewModel.greenWallet))
                        onDismissRequest()
                    }
                }
            }
        }
    }
}
