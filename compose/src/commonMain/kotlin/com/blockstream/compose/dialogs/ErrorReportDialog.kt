package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_email
import blockstream_green.common.generated.resources.id_feedback
import blockstream_green.common.generated.resources.id_optional
import blockstream_green.common.generated.resources.id_send
import blockstream_green.common.generated.resources.id_send_error_report
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.events.Events
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.titleSmall
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
fun ErrorReportDialog(
    errorReport: ErrorReport,
    onSubmitErrorReport: ((Events.SubmitErrorReport) -> Unit)? = null,
    onDismiss: () -> Unit = { }
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        GreenCard {
            GreenColumn(padding = 8) {
                Text(
                    text = stringResource(Res.string.id_send_error_report),
                    style = titleSmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )

                var email by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    supportingText = { Text(text = stringResource(Res.string.id_optional)) },
                    label = { Text(stringResource(Res.string.id_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true,
                )


                var message by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it
                    },
                    label = { Text(stringResource(Res.string.id_feedback)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    GreenButton(
                        text = stringResource(Res.string.id_cancel),
                        type = GreenButtonType.TEXT
                    ) {
                        onDismiss()
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_send),
                        type = GreenButtonType.TEXT,
                        enabled = email.isNotBlank()
                    ) {
                        onSubmitErrorReport?.invoke(
                            Events.SubmitErrorReport(
                                email = email,
                                message = message,
                                errorReport = errorReport
                            )
                        )
                        onDismiss()
                    }
                }
            }
        }
    }
}
