package com.blockstream.compose.screens.support

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_email
import blockstream_green.common.generated.resources.id_i_understand_that_asking_for
import blockstream_green.common.generated.resources.id_issue_description
import blockstream_green.common.generated.resources.id_message
import blockstream_green.common.generated.resources.id_please_be_as_detailed_as_possible
import blockstream_green.common.generated.resources.id_share_logs
import blockstream_green.common.generated.resources.id_submit
import com.blockstream.common.SupportType
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.support.SupportViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.components.GreenColumn
import org.jetbrains.compose.resources.stringResource

@Composable
fun SupportScreen(
    viewModel: SupportViewModelAbstract
) {

    val type = viewModel.type
    val email by viewModel.email.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val attachLogs by viewModel.attachLogs.collectAsStateWithLifecycle()
    val torAcknowledged by viewModel.torAcknowledged.collectAsStateWithLifecycle()
    val isTorEnabled by viewModel.isTorEnabled.collectAsStateWithLifecycle()
    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()

    SetupScreen(viewModel = viewModel, withPadding = false) {

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            GreenColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                GreenColumn(
                    padding = 0,
                    space = 8,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {

                    if (viewModel.type == SupportType.INCIDENT) {
                        Text(
                            stringResource(Res.string.id_please_be_as_detailed_as_possible),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = viewModel.email.onValueChange(),
                        label = { Text(stringResource(Res.string.id_email)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = viewModel.message.onValueChange(maxChars = 1000),
                        label = { Text(stringResource(if (viewModel.type == SupportType.INCIDENT) Res.string.id_issue_description else Res.string.id_message)) },
                        supportingText = {
                            Text(
                                text = "${message.length} / 1000",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 350.dp)
                    )
                }

                GreenColumn(padding = 0, space = 8) {

                    Column {

                        if (type == SupportType.INCIDENT) {
                            Row(
                                modifier = Modifier.toggleable(
                                    value = attachLogs,
                                    onValueChange = viewModel.attachLogs.onValueChange(),
                                    role = Role.Checkbox
                                ).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = attachLogs,
                                    onCheckedChange = viewModel.attachLogs.onValueChange(),
                                )

                                Text(stringResource(Res.string.id_share_logs))
                            }
                        }


                        if (isTorEnabled) {
                            Row(
                                modifier = Modifier.toggleable(
                                    value = torAcknowledged,
                                    onValueChange = viewModel.torAcknowledged.onValueChange(),
                                    role = Role.Checkbox
                                ).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = torAcknowledged,
                                    onCheckedChange = viewModel.torAcknowledged.onValueChange()
                                )

                                Text(stringResource(Res.string.id_i_understand_that_asking_for))
                            }
                        }
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_submit),
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = buttonEnabled,
                        onProgress = onProgress
                    ) {
                        viewModel.postEvent(Events.Continue)
                    }
                }
            }
        }
    }
}