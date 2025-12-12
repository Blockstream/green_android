package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
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
import blockstream_green.common.generated.resources.id_choose_a_name_for_your_wallet
import blockstream_green.common.generated.resources.id_save
import blockstream_green.common.generated.resources.id_wallet_name
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.wallet.WalletNameViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.extensions.onTextFieldValueChange
import com.blockstream.compose.utils.OpenKeyboard
import com.blockstream.compose.utils.appTestTag
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletRenameBottomSheet(
    viewModel: WalletNameViewModelAbstract,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_wallet_name),
        subtitle = stringResource(Res.string.id_choose_a_name_for_your_wallet),
        viewModel = viewModel,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {

        val focusRequester = remember { FocusRequester() }
        OpenKeyboard(focusRequester)

        val name by viewModel.name.collectAsStateWithLifecycle()

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
            onValueChange = viewModel.name.onTextFieldValueChange {
                textFieldValueState = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .appTestTag("wallet_name_textfield"),
            singleLine = true,
            label = { Text(stringResource(Res.string.id_wallet_name)) },
            trailingIcon = {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "clear text",
                    modifier = Modifier
                        .clickable {
                            viewModel.name.value = ""
                        }
                        .appTestTag("clear_text")
                )
            }
        )

        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
        GreenButton(
            text = stringResource(Res.string.id_save),
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonEnabled
        ) {
            viewModel.postEvent(Events.Continue)
        }
    }
}
