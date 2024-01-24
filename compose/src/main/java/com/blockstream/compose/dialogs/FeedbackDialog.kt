package com.blockstream.compose.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.common.models.about.AboutViewModel
import com.blockstream.common.models.about.AboutViewModelAbstract
import com.blockstream.common.models.about.AboutViewModelPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleSmall


@Composable
fun FeedbackDialog(
    viewModel: AboutViewModelAbstract,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        GreenCard {
            GreenColumn(padding = 8) {
                Text(
                    text = stringResource(id = R.string.id_give_us_your_feedback),
                    style = titleSmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(id = R.string.id_rate_your_experience),
                    style = bodyMedium,
                )

                val selectedRate by viewModel.rate.collectAsStateWithLifecycle()
                SingleChoiceSegmentedButtonRow {
                    (1..5).forEachIndexed { index, rate ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index, count = 5
                            ),
                            onClick = {
                                viewModel.rate.value = rate
                            },
                            selected = (index + 1) == selectedRate
                        ) {
                            Text(rate.toString())
                        }
                    }
                }

                val email by viewModel.email.collectAsStateWithLifecycle()
                OutlinedTextField(
                    value = email,
                    onValueChange = viewModel.email.onValueChange(),
                    supportingText = { Text(text = stringResource(id = R.string.id_optional)) },
                    label = { Text(stringResource(R.string.id_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true,
                )


                val feedback by viewModel.feedback.collectAsStateWithLifecycle()
                OutlinedTextField(
                    value = feedback,
                    onValueChange = viewModel.feedback.onValueChange(),
                    label = { Text(stringResource(R.string.id_feedback)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    GreenButton(
                        text = stringResource(id = R.string.id_cancel),
                        type = GreenButtonType.TEXT
                    ) {
                        onDismissRequest()
                    }

                    val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
                    GreenButton(
                        text = stringResource(id = R.string.id_send),
                        type = GreenButtonType.TEXT,
                        enabled = buttonEnabled
                    ) {
                        viewModel.postEvent(AboutViewModel.LocalEvents.SendFeedback)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun FeedbackDialogPreview() {
    GreenThemePreview {
        FeedbackDialog(viewModel = AboutViewModelPreview.preview()) {

        }
    }
}