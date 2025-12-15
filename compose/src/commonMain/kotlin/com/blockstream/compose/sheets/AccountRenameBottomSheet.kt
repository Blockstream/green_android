package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_name
import blockstream_green.common.generated.resources.id_save
import com.blockstream.compose.events.Events
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.utils.OpenKeyboard
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountRenameBottomSheet(
    viewModel: GreenViewModel,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_account_name),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        val focusRequester = remember { FocusRequester() }
        OpenKeyboard(focusRequester)

        var name by remember {
            mutableStateOf(viewModel.account.name)
        }

        // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
        // of the composition.
        var textFieldValueState by remember {
            mutableStateOf(
                TextFieldValue(
                    text = name, selection = when {
                        name.isEmpty() -> TextRange.Zero
                        else -> TextRange(name.length)
                    }
                )
            )
        }

        // Holds the latest TextFieldValue that BasicTextField was recomposed with. We couldn't simply
        // pass `TextFieldValue(text = value)` to the CoreTextField because we need to preserve the
        // composition.
        val textFieldValue = textFieldValueState.copy(text = name)

        SideEffect {
            if (textFieldValue.selection != textFieldValueState.selection ||
                textFieldValue.composition != textFieldValueState.composition
            ) {
                textFieldValueState = textFieldValue
            }
        }

        TextField(
            value = textFieldValueState,
            onValueChange = {
                name = it.text
                textFieldValueState = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            label = { Text(stringResource(Res.string.id_account_name)) },
            trailingIcon = {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "clear text",
                    modifier = Modifier
                        .clickable {
                            name = ""
                        }
                )
            }
        )

        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
        GreenButton(
            text = stringResource(Res.string.id_save),
            modifier = Modifier.fillMaxWidth(),
            enabled = !onProgress && name.isNotBlank()
        ) {
            viewModel.postEvent(Events.RenameAccount(viewModel.account, name))
        }
    }
}