package com.blockstream.compose.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_allows_you_to_quickly_check
import blockstream_green.common.generated.resources.id_at_least_8_characters_required
import blockstream_green.common.generated.resources.id_delete_credentials
import blockstream_green.common.generated.resources.id_password
import blockstream_green.common.generated.resources.id_save
import blockstream_green.common.generated.resources.id_update
import blockstream_green.common.generated.resources.id_username
import blockstream_green.common.generated.resources.id_watchonly_credentials
import com.blockstream.compose.events.Events
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.compose.models.settings.WatchOnlyCredentialsSettingsViewModel
import com.blockstream.compose.models.settings.WatchOnlyCredentialsSettingsViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.TextInputPassword
import com.blockstream.compose.utils.TextInputPaste
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchOnlyCredentialsSettingsBottomSheet(
    viewModel: WatchOnlyCredentialsSettingsViewModelAbstract,
    onDismissRequest: () -> Unit,
) {

    GreenBottomSheet(
        title = stringResource(Res.string.id_watchonly_credentials),
        subtitle = viewModel.network.name,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        Text(
            text = stringResource(Res.string.id_allows_you_to_quickly_check),
            style = bodyMedium,
            color = whiteMedium
        )

        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current
        val username by viewModel.username.collectAsStateWithLifecycle()

        TextField(
            value = username,
            onValueChange = viewModel.username.onValueChange(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            isError = username.isNotBlank() && username.length < 8,
            label = { Text(stringResource(Res.string.id_username)) },
            supportingText = {
                Text(if (username.length < 8) stringResource(Res.string.id_at_least_8_characters_required) else "")
            },
            trailingIcon = {
                TextInputPaste(viewModel.username)
            }
        )

        val password by viewModel.password.collectAsStateWithLifecycle()
        val passwordVisibility = remember { mutableStateOf(false) }

        TextField(
            value = password,
            visualTransformation = if (passwordVisibility.value) VisualTransformation.None else PasswordVisualTransformation(),
            onValueChange = viewModel.password.onValueChange(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            isError = password.isNotBlank() && password.length < 8,
            label = { Text(stringResource(Res.string.id_password)) },
            supportingText = {
                Text(if (password.length < 8) stringResource(Res.string.id_at_least_8_characters_required) else "")
            },
            trailingIcon = {
                TextInputPassword(passwordVisibility)
            }
        )

        val hasWatchOnlyCredentials by viewModel.hasWatchOnlyCredentials.collectAsStateWithLifecycle()
        val buttonEnabled by viewModel.buttonEnabled.collectAsStateWithLifecycle()
        GreenButton(
            text = stringResource(if (hasWatchOnlyCredentials) Res.string.id_update else Res.string.id_save),
            enabled = buttonEnabled,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            viewModel.postEvent(Events.Continue)
        }

        var isDeleteConfirmed by remember { mutableStateOf(false) }

        AnimatedVisibility(visible = hasWatchOnlyCredentials) {
            GreenButton(
                text = stringResource(Res.string.id_delete_credentials),
                modifier = Modifier.fillMaxWidth(),
                type = if (isDeleteConfirmed) GreenButtonType.COLOR else GreenButtonType.OUTLINE,
                color = GreenButtonColor.RED
            ) {
                if (isDeleteConfirmed) {
                    viewModel.postEvent(WatchOnlyCredentialsSettingsViewModel.LocalEvents.DeleteCredentials)
                } else {
                    isDeleteConfirmed = true

                    scope.launch {
                        delay(3000L)
                        isDeleteConfirmed = false
                    }
                }
            }
        }
    }
}