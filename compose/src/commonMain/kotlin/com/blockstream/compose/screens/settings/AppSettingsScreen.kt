@file:OptIn(ExperimentalComposeUiApi::class)

package com.blockstream.compose.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_advanced
import blockstream_green.common.generated.resources.id_connect_through_a_proxy
import blockstream_green.common.generated.resources.id_connect_with_tor
import blockstream_green.common.generated.resources.id_electrum_server_gap_limit
import blockstream_green.common.generated.resources.id_enable_experimental_features
import blockstream_green.common.generated.resources.id_enable_testnet
import blockstream_green.common.generated.resources.id_enhanced_privacy
import blockstream_green.common.generated.resources.id_host_ip
import blockstream_green.common.generated.resources.id_language
import blockstream_green.common.generated.resources.id_number_of_consecutive_empty
import blockstream_green.common.generated.resources.id_personal_electrum_server
import blockstream_green.common.generated.resources.id_remember_hardware_devices
import blockstream_green.common.generated.resources.id_screen_lock
import blockstream_green.common.generated.resources.id_system_default
import blockstream_green.common.generated.resources.id_use_secure_display_and_screen
import blockstream_green.common.generated.resources.id_your_settings_are_unsavednndo
import blockstream_green.common.generated.resources.id_less_stable_connection
import blockstream_green.common.generated.resources.id_help_us_improve
import blockstream_green.common.generated.resources.id_enable_limited_usage_data
import blockstream_green.common.generated.resources.id_more_info
import blockstream_green.common.generated.resources.id_experimental_features_might
import blockstream_green.common.generated.resources.id_custom_server_settings
import blockstream_green.common.generated.resources.id_reset_to_default
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.blockstream.common.data.ScreenLockSetting
import com.blockstream.compose.models.settings.AppSettingsViewModel
import com.blockstream.compose.models.settings.AppSettingsViewModelAbstract
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.screens.settings.components.PersonalElectrumServerSection
import com.blockstream.compose.screens.settings.components.SettingSwitch
import com.blockstream.compose.screens.settings.components.SettingsItem
import com.blockstream.compose.sheets.LanguagePickerBottomSheet
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.TextInputPaste
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.RichSpan
import com.blockstream.compose.components.RichText
import com.blockstream.compose.utils.appTestTag
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModelAbstract
) {
    // Helper function to handle value changes and auto-save for Boolean
    fun autoSaveOnBooleanChange(stateFlow: kotlinx.coroutines.flow.MutableStateFlow<Boolean>): (Boolean) -> Unit = { newValue ->
        stateFlow.value = newValue
        viewModel.postEvent(AppSettingsViewModel.LocalEvents.AutoSave)
    }

    // Helper function to handle value changes and auto-save for String
    fun autoSaveOnStringChange(stateFlow: kotlinx.coroutines.flow.MutableStateFlow<String>): (String) -> Unit = { newValue ->
        stateFlow.value = newValue
        viewModel.postEvent(AppSettingsViewModel.LocalEvents.AutoSave)
    }

    // Helper function to handle value changes and auto-save for ScreenLockSetting
    fun autoSaveOnScreenLockChange(stateFlow: kotlinx.coroutines.flow.MutableStateFlow<ScreenLockSetting>): (ScreenLockSetting) -> Unit = { newValue ->
        stateFlow.value = newValue
        viewModel.postEvent(AppSettingsViewModel.LocalEvents.AutoSave)
    }

    val dialog = LocalDialog.current

    SetupScreen(viewModel = viewModel, withPadding = false, scrollable = false, sideEffectsHandler = {
        if (it is AppSettingsViewModel.LocalSideEffects.UnsavedAppSettings) {
            val openDialogData =
                OpenDialogData(
                    title = StringHolder.create(Res.string.id_advanced),
                    message = StringHolder.create(
                        Res.string.id_your_settings_are_unsavednndo
                    ),
                    onPrimary = {
                        viewModel.postEvent(AppSettingsViewModel.LocalEvents.Cancel)
                    },
                    onSecondary = {

                    })

            launch {
                dialog.openDialog(openDialogData)
            }
        }
    }) {

        Box {
            GreenColumn(
                padding = 0,
                space = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Connect with Tor
                val torEnabled by viewModel.torEnabled.collectAsStateWithLifecycle()
                SettingSwitch(
                    title = stringResource(Res.string.id_connect_with_tor),
                    subtitle = stringResource(Res.string.id_less_stable_connection),
                    checked = torEnabled,
                    onCheckedChange = autoSaveOnBooleanChange(viewModel.torEnabled),
                    testTag = "tor_switch"
                )


                // Connect through a proxy
                Column {
                    val proxyEnabled by viewModel.proxyEnabled.collectAsStateWithLifecycle()
                    SettingSwitch(
                        title = stringResource(Res.string.id_connect_through_a_proxy),
                        checked = proxyEnabled,
                        onCheckedChange = autoSaveOnBooleanChange(viewModel.proxyEnabled),
                        testTag = "proxy_switch"
                    )

                    AnimatedVisibility(visible = proxyEnabled) {
                        val proxyUrl by viewModel.proxyUrl.collectAsStateWithLifecycle()
                        OutlinedTextField(
                            value = proxyUrl,
                            onValueChange = autoSaveOnStringChange(viewModel.proxyUrl),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            singleLine = true,
                            supportingText = { Text(stringResource(Res.string.id_host_ip)) },
                            isError = proxyUrl.isBlank(),
                            trailingIcon = {
                                TextInputPaste(state = viewModel.proxyUrl)
                            }
                        )
                    }
                }


                // Remember hardware devices
                val rememberHardwareDevices by viewModel.rememberHardwareDevices.collectAsStateWithLifecycle()
                SettingSwitch(
                    title = stringResource(Res.string.id_remember_hardware_devices),
                    checked = rememberHardwareDevices,
                    onCheckedChange = autoSaveOnBooleanChange(viewModel.rememberHardwareDevices),
                    testTag = "remember_devices_switch"
                )


                // Enable testnet
                val testnetEnabled by viewModel.testnetEnabled.collectAsStateWithLifecycle()
                SettingSwitch(
                    title = stringResource(Res.string.id_enable_testnet),
                    checked = testnetEnabled,
                    onCheckedChange = autoSaveOnBooleanChange(viewModel.testnetEnabled),
                    testTag = "enable_testnet_switch"
                )


                if (viewModel.analyticsFeatureEnabled) {
                    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
                    OutlinedCard {
                        Box(modifier = Modifier.padding(16.dp)) {
                            GreenRow(
                                space = 16,
                                padding = 0,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(Res.string.id_help_us_improve),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    RichText(
                                        text = stringResource(Res.string.id_enable_limited_usage_data) + "\n" + stringResource(Res.string.id_more_info),
                                        spans = listOf(
                                            RichSpan(
                                                text = stringResource(Res.string.id_more_info),
                                                style = SpanStyle(color = MaterialTheme.colorScheme.primary),
                                                onClick = {
                                                    viewModel.postEvent(AppSettingsViewModel.LocalEvents.AnalyticsMoreInfo)
                                                }
                                            )
                                        ),
                                        paragraph = ParagraphStyle(textAlign = TextAlign.Start),
                                        defaultStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                Switch(
                                    checked = analyticsEnabled,
                                    onCheckedChange = autoSaveOnBooleanChange(viewModel.analyticsEnabled),
                                    colors = SwitchDefaults.colors(
                                        uncheckedThumbColor = Color.White,
                                        checkedThumbColor = Color.White,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.appTestTag("analytics_switch"),
                                )
                            }
                        }
                    }
                    }

                // Experimental features
                if (viewModel.experimentalFeatureEnabled) {
                    val experimentalFeaturesEnabled by viewModel.experimentalFeaturesEnabled.collectAsStateWithLifecycle()
                    SettingSwitch(
                        title = stringResource(Res.string.id_enable_experimental_features),
                        subtitle = stringResource(Res.string.id_experimental_features_might),
                        checked = experimentalFeaturesEnabled,
                        onCheckedChange = autoSaveOnBooleanChange(viewModel.experimentalFeaturesEnabled),
                        testTag = "experimental_switch"

                    )
                }

                // Language
                var showLanguagePicker by remember { mutableStateOf(false) }
                val locales by viewModel.locales.collectAsStateWithLifecycle()
                val locale by viewModel.locale.collectAsStateWithLifecycle()

                SettingsItem(
                    title = stringResource(Res.string.id_language),
                    subtitle = locale?.let { locales[it] } ?: stringResource(Res.string.id_system_default),
                    onClick = {
                        showLanguagePicker = true
                    },
                    rightContent = {
                        Icon(
                            imageVector = PhosphorIcons.Regular.CaretRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.appTestTag("language_button"),
                )
                
                if (showLanguagePicker) {
                    LanguagePickerBottomSheet(
                        viewModel = viewModel,
                        onDismissRequest = {
                            showLanguagePicker = false
                        }
                    )
                }


                // Custom Server Settings section
                Text(
                    text = stringResource(Res.string.id_custom_server_settings),
                    style = titleLarge,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                )

                val electrumNodeEnabled by viewModel.electrumNodeEnabled.collectAsStateWithLifecycle()

                SettingSwitch(
                    title = stringResource(Res.string.id_personal_electrum_server),
                    checked = electrumNodeEnabled,
                    onCheckedChange = autoSaveOnBooleanChange(viewModel.electrumNodeEnabled),
                    testTag = "electrum_switch"
                )

                AnimatedVisibility(visible = electrumNodeEnabled) {
                    PersonalElectrumServerSection(
                        viewModel = viewModel,
                        testnetEnabled = testnetEnabled,
                        autoSaveOnBooleanChange = ::autoSaveOnBooleanChange,
                        autoSaveOnStringChange = ::autoSaveOnStringChange,
                    )
                }


                val electrumServerGapLimit by viewModel.electrumServerGapLimit.collectAsStateWithLifecycle()
                val focusManager = LocalFocusManager.current

                SettingsItem(
                    title = stringResource(Res.string.id_electrum_server_gap_limit),
                    subtitle = stringResource(Res.string.id_number_of_consecutive_empty),
                    content = {
                        OutlinedTextField(
                            value = electrumServerGapLimit,
                            onValueChange = autoSaveOnStringChange(viewModel.electrumServerGapLimit),
                            modifier = Modifier
                                .fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            trailingIcon = {
                                if (electrumServerGapLimit.isNotEmpty() && electrumServerGapLimit != "20") {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(Res.string.id_reset_to_default),
                                        modifier = Modifier.clickable {
                                            viewModel.electrumServerGapLimit.value = "20"
                                            viewModel.postEvent(AppSettingsViewModel.LocalEvents.AutoSave)
                                        }
                                    )
                                }
                            }
                        )
                    }
                )


                // Enhanced Privacy (placeholder for SPV verification in the future)
                val enhancedPrivacyEnabled by viewModel.enhancedPrivacyEnabled.collectAsStateWithLifecycle()
                SettingSwitch(
                    title = stringResource(Res.string.id_enhanced_privacy),
                    subtitle = stringResource(Res.string.id_use_secure_display_and_screen),
                    checked = enhancedPrivacyEnabled,
                    onCheckedChange = autoSaveOnBooleanChange(viewModel.enhancedPrivacyEnabled),
                    testTag = "enhanced_privacy_switch"
                )

                AnimatedVisibility(visible = enhancedPrivacyEnabled) {
                    val screenLockSettings = ScreenLockSetting.getStringList().map {
                        stringResource(it)
                    }
                    val screenLockInSeconds by viewModel.screenLockInSeconds.collectAsStateWithLifecycle()

                    var screenLockExpanded by remember { mutableStateOf(false) }
                    val selectedOptionText by remember {
                        derivedStateOf {
                            screenLockSettings[screenLockInSeconds.ordinal]
                        }
                    }

                    // We want to react on tap/press on TextField to show menu
                    ExposedDropdownMenuBox(
                        expanded = screenLockExpanded,
                        onExpandedChange = { screenLockExpanded = it },
                        modifier = Modifier
                            .padding(start = 54.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            // The `menuAnchor` modifier must be passed to the text field for correctness.
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = selectedOptionText,
                            onValueChange = {},
                            label = { Text(stringResource(Res.string.id_screen_lock)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = screenLockExpanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = screenLockExpanded,
                            onDismissRequest = { screenLockExpanded = false },
                        ) {
                            screenLockSettings.forEachIndexed { index, selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        ScreenLockSetting.byPosition(index).let {
                                            viewModel.screenLockInSeconds.value = it
                                            viewModel.postEvent(AppSettingsViewModel.LocalEvents.AutoSave)
                                        }
                                        screenLockExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
