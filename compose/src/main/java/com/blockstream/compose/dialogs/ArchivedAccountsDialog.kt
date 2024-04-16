package com.blockstream.compose.dialogs

import android.view.LayoutInflater
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import app.rive.runtime.kotlin.RiveAnimationView
import com.blockstream.common.events.Events
import com.blockstream.common.models.archived.ArchivedAccountsViewModelAbstract
import com.blockstream.common.models.archived.ArchivedAccountsViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
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
                    AndroidView({
                        LayoutInflater.from(it)
                            .inflate(R.layout.rive, null)
                            .apply {
                                val animationView: RiveAnimationView = findViewById(R.id.rive)
                                animationView.setRiveResource(
                                    R.raw.account_archived,
                                    autoplay = true
                                )
                            }
                    })
                }

                GreenColumn(padding = 0, space = 8, modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        text = stringResource(id = R.string.id_account_archived),
                        style = titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(id = R.string.id_you_can_still_receive_funds_but),
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
                        text = stringResource(id = R.string.id_continue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onDismissRequest()
                    }

                    val archivedAccounts by viewModel.archivedAccounts.collectAsState()

                    GreenButton(
                        text = stringResource(
                            R.string.id_see_archived_accounts_s,
                            archivedAccounts.size.toString()
                        ),
                        type = GreenButtonType.OUTLINE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.postEvent(NavigateDestinations.ArchivedAccounts)
                        onDismissRequest()
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun ArchivedAccountsDialogPreview() {
    GreenThemePreview {
        ArchivedAccountsDialog(
            viewModel = ArchivedAccountsViewModelPreview.preview()
        ) {

        }
    }
}