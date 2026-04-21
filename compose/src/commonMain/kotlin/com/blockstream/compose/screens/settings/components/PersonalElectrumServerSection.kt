package com.blockstream.compose.screens.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_bitcoin_electrum_server
import blockstream_green.common.generated.resources.id_enable_tls
import blockstream_green.common.generated.resources.id_liquid_electrum_server
import blockstream_green.common.generated.resources.id_liquid_testnet_electrum_server
import blockstream_green.common.generated.resources.id_testnet_electrum_server
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.models.settings.AppSettingsViewModelAbstract
import com.blockstream.compose.utils.TextInputPaste
import com.blockstream.compose.utils.appTestTag
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
fun PersonalElectrumServerSection(
    viewModel: AppSettingsViewModelAbstract,
    testnetEnabled: Boolean,
    autoSaveOnBooleanChange: (MutableStateFlow<Boolean>) -> (Boolean) -> Unit,
    autoSaveOnStringChange: (
        MutableStateFlow<String>,
        errorStateFlow: MutableStateFlow<String?>?
    ) -> (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    val personalElectrumServerTlsEnabled by viewModel.personalElectrumServerTlsEnabled.collectAsStateWithLifecycle()
    val personalBitcoinElectrumServer by viewModel.personalBitcoinElectrumServer.collectAsStateWithLifecycle()
    val personalLiquidElectrumServer by viewModel.personalLiquidElectrumServer.collectAsStateWithLifecycle()
    val personalTestnetElectrumServer by viewModel.personalTestnetElectrumServer.collectAsStateWithLifecycle()
    val personalTestnetLiquidElectrumServer by viewModel.personalTestnetLiquidElectrumServer.collectAsStateWithLifecycle()
    val bitcoinError by viewModel.personalBitcoinElectrumServerError.collectAsStateWithLifecycle()
    val liquidError by viewModel.personalLiquidElectrumServerError.collectAsStateWithLifecycle()

    val testnetBitcoinError by viewModel.personalTestnetElectrumServerError.collectAsStateWithLifecycle()
    val testnetLiquidError by viewModel.personalTestnetLiquidElectrumServerError.collectAsStateWithLifecycle()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        GreenColumn(
            space = 8,
            padding = 16,
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingSwitch(
                title = stringResource(Res.string.id_enable_tls),
                checked = personalElectrumServerTlsEnabled,
                onCheckedChange = autoSaveOnBooleanChange(viewModel.personalElectrumServerTlsEnabled),
                testTag = "tls_switch"
            )

            SettingsItem(
                title = stringResource(Res.string.id_bitcoin_electrum_server),
                content = {
                    OutlinedTextField(
                        value = personalBitcoinElectrumServer,
                        onValueChange = autoSaveOnStringChange(
                            viewModel.personalBitcoinElectrumServer,
                            viewModel.personalBitcoinElectrumServerError
                        ),
                        modifier = Modifier.fillMaxWidth().appTestTag("bitcoin_electrum_server_textfield"),
                        placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_BITCOIN_ELECTRUM_URL) },
                        singleLine = true,
                        isError = bitcoinError != null,
                        supportingText = {
                            if (bitcoinError != null) Text(bitcoinError!!)
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        trailingIcon = {
                            TextInputPaste(
                                state = viewModel.personalBitcoinElectrumServer,
                                onValueChange = autoSaveOnStringChange(
                                    viewModel.personalBitcoinElectrumServer,
                                    viewModel.personalBitcoinElectrumServerError
                                )
                            )
                        }
                    )
                }
            )

            SettingsItem(
                title = stringResource(Res.string.id_liquid_electrum_server),
                content = {
                    OutlinedTextField(
                        value = personalLiquidElectrumServer,
                        onValueChange = autoSaveOnStringChange(
                            viewModel.personalLiquidElectrumServer,
                            viewModel.personalLiquidElectrumServerError
                        ),
                        modifier = Modifier.fillMaxWidth().appTestTag("liquid_electrum_server_textfield"),
                        placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_LIQUID_ELECTRUM_URL) },
                        singleLine = true,
                        isError = liquidError != null,
                        supportingText = {
                            if (liquidError != null) Text(liquidError!!)
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        trailingIcon = {
                            TextInputPaste(
                                state = viewModel.personalLiquidElectrumServer,
                                onValueChange = autoSaveOnStringChange(
                                    viewModel.personalLiquidElectrumServer,
                                    viewModel.personalLiquidElectrumServerError
                                )
                            )
                        }
                    )
                }
            )

            if (testnetEnabled) {
                SettingsItem(
                    title = stringResource(Res.string.id_testnet_electrum_server),
                    content = {
                        OutlinedTextField(
                            value = personalTestnetElectrumServer,
                            onValueChange = autoSaveOnStringChange(
                                viewModel.personalTestnetElectrumServer,
                                viewModel.personalTestnetElectrumServerError
                            ),
                            isError = testnetBitcoinError != null,
                            supportingText = {
                                if (testnetBitcoinError != null) {
                                    Text(testnetBitcoinError!!)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().appTestTag("bitcoin_testnet_electrum_server_textfield"),
                            placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_TESTNET_ELECTRUM_URL) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            trailingIcon = {
                                TextInputPaste(
                                    state = viewModel.personalTestnetElectrumServer,
                                    onValueChange = autoSaveOnStringChange(
                                        viewModel.personalTestnetElectrumServer,
                                        viewModel.personalTestnetElectrumServerError
                                    )
                                )
                            }
                        )
                    }
                )

                SettingsItem(
                    title = stringResource(Res.string.id_liquid_testnet_electrum_server),
                    content = {
                        OutlinedTextField(
                            value = personalTestnetLiquidElectrumServer,
                            onValueChange = autoSaveOnStringChange(
                                viewModel.personalTestnetLiquidElectrumServer,
                                viewModel.personalTestnetLiquidElectrumServerError
                            ),
                            isError = testnetLiquidError != null,
                            supportingText = {
                                if (testnetLiquidError != null) {
                                    Text(testnetLiquidError!!)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().appTestTag("liquid_testnet_electrum_server_textfield"),
                            placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_TESTNET_LIQUID_ELECTRUM_URL) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            trailingIcon = {
                                TextInputPaste(
                                    state = viewModel.personalTestnetLiquidElectrumServer,
                                    onValueChange = autoSaveOnStringChange(
                                        viewModel.personalTestnetLiquidElectrumServer,
                                        viewModel.personalTestnetLiquidElectrumServerError
                                    )
                                )
                            }
                        )
                    }
                )
            }
        }
    }
}