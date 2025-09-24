package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_bitcoin
import blockstream_green.common.generated.resources.id_enter_credentials_to_access_multisig
import blockstream_green.common.generated.resources.id_liquid
import blockstream_green.common.generated.resources.id_set_up_s_multisig_watchonly
import blockstream_green.common.generated.resources.id_import_wallet
import blockstream_green.common.generated.resources.id_password
import blockstream_green.common.generated.resources.id_remember_me
import blockstream_green.common.generated.resources.id_set_up_multisig_watchonly
import blockstream_green.common.generated.resources.id_username
import com.blockstream.common.events.Events
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyMultisigViewModelAbstract
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.TextInputPassword
import com.blockstream.compose.utils.TextInputPaste
import com.blockstream.compose.utils.noRippleToggleable
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchOnlyMultisigScreen(
    viewModel: WatchOnlyMultisigViewModelAbstract
) {
    SetupScreen(viewModel, withPadding = false) {
        GreenColumn(
            padding = 0,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            val focusManager = LocalFocusManager.current

            GreenColumn(
                padding = 0,
                modifier = Modifier.weight(1f)
            ) {
                val networkName = when {
                    viewModel.setupArgs.network?.isBitcoin == true -> stringResource(Res.string.id_bitcoin)
                    viewModel.setupArgs.network?.isLiquid == true -> stringResource(Res.string.id_liquid)
                    else -> null
                }

                Text(
                    text = if (networkName != null) {
                        stringResource(Res.string.id_set_up_s_multisig_watchonly, networkName)
                    } else {
                        stringResource(Res.string.id_set_up_multisig_watchonly)
                    },
                    style = displayMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(Res.string.id_enter_credentials_to_access_multisig),
                    style = bodyLarge,
                    color = whiteMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                GreenColumn(
                    padding = 0,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
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
                        label = { Text(stringResource(Res.string.id_username)) },
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
                        label = { Text(stringResource(Res.string.id_password)) },
                        trailingIcon = {
                            TextInputPassword(passwordVisibility)
                        }
                    )

                    Column {
                        val isRememberMe by viewModel.isRememberMe.collectAsStateWithLifecycle()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.noRippleToggleable(
                                value = isRememberMe,
                                onValueChange = viewModel.isRememberMe.onValueChange()
                            )
                        ) {
                            Text(
                                text = stringResource(Res.string.id_remember_me),
                                style = bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = isRememberMe,
                                onCheckedChange = viewModel.isRememberMe.onValueChange()
                            )
                        }
                    }

                    val isLoginEnabled by viewModel.isLoginEnabled.collectAsStateWithLifecycle()
                    Column {
                        GreenButton(
                            text = stringResource(Res.string.id_import_wallet),
                            size = GreenButtonSize.BIG,
                            enabled = isLoginEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.postEvent(Events.Continue)
                        }
                    }
                }
            }
        }
    }
}