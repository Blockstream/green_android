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
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract
import com.blockstream.compose.utils.TextInputPaste
import com.blockstream.ui.components.GreenColumn
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
fun PersonalElectrumServerSection(
    viewModel: AppSettingsViewModelAbstract,
    testnetEnabled: Boolean,
    autoSaveOnBooleanChange: (MutableStateFlow<Boolean>) -> (Boolean) -> Unit,
    autoSaveOnStringChange: (MutableStateFlow<String>) -> (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    val personalElectrumServerTlsEnabled by viewModel.personalElectrumServerTlsEnabled.collectAsStateWithLifecycle()
    val personalBitcoinElectrumServer by viewModel.personalBitcoinElectrumServer.collectAsStateWithLifecycle()
    val personalLiquidElectrumServer by viewModel.personalLiquidElectrumServer.collectAsStateWithLifecycle()
    val personalTestnetElectrumServer by viewModel.personalTestnetElectrumServer.collectAsStateWithLifecycle()
    val personalTestnetLiquidElectrumServer by viewModel.personalTestnetLiquidElectrumServer.collectAsStateWithLifecycle()
    
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
                onCheckedChange = autoSaveOnBooleanChange(viewModel.personalElectrumServerTlsEnabled)
            )

            SettingsItem(
                title = stringResource(Res.string.id_bitcoin_electrum_server),
                content = {
                    OutlinedTextField(
                        value = personalBitcoinElectrumServer,
                        onValueChange = autoSaveOnStringChange(viewModel.personalBitcoinElectrumServer),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_BITCOIN_ELECTRUM_URL) },
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
                            TextInputPaste(state = viewModel.personalBitcoinElectrumServer)
                        }
                    )
                }
            )

            SettingsItem(
                title = stringResource(Res.string.id_liquid_electrum_server),
                content = {
                    OutlinedTextField(
                        value = personalLiquidElectrumServer,
                        onValueChange = autoSaveOnStringChange(viewModel.personalLiquidElectrumServer),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_LIQUID_ELECTRUM_URL) },
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
                            TextInputPaste(state = viewModel.personalLiquidElectrumServer)
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
                            onValueChange = autoSaveOnStringChange(viewModel.personalTestnetElectrumServer),
                            modifier = Modifier.fillMaxWidth(),
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
                                TextInputPaste(state = viewModel.personalTestnetElectrumServer)
                            }
                        )
                    }
                )

                SettingsItem(
                    title = stringResource(Res.string.id_liquid_testnet_electrum_server),
                    content = {
                        OutlinedTextField(
                            value = personalTestnetLiquidElectrumServer,
                            onValueChange = autoSaveOnStringChange(viewModel.personalTestnetLiquidElectrumServer),
                            modifier = Modifier.fillMaxWidth(),
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
                                TextInputPaste(state = viewModel.personalTestnetLiquidElectrumServer)
                            }
                        )
                    }
                )
            }
        }
    }
}