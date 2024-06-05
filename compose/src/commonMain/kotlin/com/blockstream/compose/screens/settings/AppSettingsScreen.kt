package com.blockstream.compose.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.cloud
import blockstream_green.common.generated.resources.cpu
import blockstream_green.common.generated.resources.eye_slash
import blockstream_green.common.generated.resources.flask_fill
import blockstream_green.common.generated.resources.funnel
import blockstream_green.common.generated.resources.hard_drives
import blockstream_green.common.generated.resources.id_app_settings
import blockstream_green.common.generated.resources.id_bitcoin_electrum_server
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_choose_the_electrum_servers_you
import blockstream_green.common.generated.resources.id_connect_through_a_proxy
import blockstream_green.common.generated.resources.id_connect_with_tor
import blockstream_green.common.generated.resources.id_custom_servers_and_validation
import blockstream_green.common.generated.resources.id_double_check_spv_with_other
import blockstream_green.common.generated.resources.id_enable_experimental_features
import blockstream_green.common.generated.resources.id_enable_limited_usage_data
import blockstream_green.common.generated.resources.id_enable_testnet
import blockstream_green.common.generated.resources.id_enhanced_privacy
import blockstream_green.common.generated.resources.id_experimental_features_might
import blockstream_green.common.generated.resources.id_help_green_improve
import blockstream_green.common.generated.resources.id_host_ip
import blockstream_green.common.generated.resources.id_liquid_electrum_server
import blockstream_green.common.generated.resources.id_liquid_testnet_electrum_server
import blockstream_green.common.generated.resources.id_more_info
import blockstream_green.common.generated.resources.id_multi_server_validation
import blockstream_green.common.generated.resources.id_personal_electrum_server
import blockstream_green.common.generated.resources.id_private_but_less_stable
import blockstream_green.common.generated.resources.id_remember_hardware_devices
import blockstream_green.common.generated.resources.id_save
import blockstream_green.common.generated.resources.id_screen_lock
import blockstream_green.common.generated.resources.id_spv_verification
import blockstream_green.common.generated.resources.id_testnet_electrum_server
import blockstream_green.common.generated.resources.id_these_settings_apply_for_every
import blockstream_green.common.generated.resources.id_use_secure_display_and_screen
import blockstream_green.common.generated.resources.id_verify_your_bitcoin
import blockstream_green.common.generated.resources.id_your_settings_are_unsavednndo
import blockstream_green.common.generated.resources.shield_check
import blockstream_green.common.generated.resources.test_tube_fill
import blockstream_green.common.generated.resources.tor
import blockstream_green.common.generated.resources.users_three
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.data.ScreenLockSetting
import com.blockstream.common.models.settings.AppSettingsViewModel
import com.blockstream.common.models.settings.AppSettingsViewModelAbstract
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenGradient
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSwitch
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.AnalyticsBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.TextInputPaste
import com.blockstream.compose.utils.ifTrue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object AppSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AppSettingsViewModel>()

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        AppSettingsScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModelAbstract
) {
    val dialog = LocalDialog.current

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    HandleSideEffect(viewModel) {
        if (it is AppSettingsViewModel.LocalSideEffects.AnalyticsMoreInfo) {
            bottomSheetNavigator?.show(AnalyticsBottomSheet)
        } else if (it is AppSettingsViewModel.LocalSideEffects.UnsavedAppSettings) {

            val openDialogData = OpenDialogData(title = StringHolder.create(Res.string.id_app_settings),
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
    }


    Box {
        GreenColumn(
            padding = 0,
            space = 0,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = stringResource(Res.string.id_these_settings_apply_for_every),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )

            val enhancedPrivacyEnabled by viewModel.enhancedPrivacyEnabled.collectAsStateWithLifecycle()

            GreenSwitch(
                title = stringResource(Res.string.id_enhanced_privacy),
                caption = stringResource(Res.string.id_use_secure_display_and_screen),
                painter = painterResource(Res.drawable.eye_slash),
                checked = enhancedPrivacyEnabled,
                onCheckedChange = viewModel.enhancedPrivacyEnabled.onValueChange()
            )

            AnimatedVisibility(visible = enhancedPrivacyEnabled) {

                val screenLockSettings = ScreenLockSetting.getStringList().map {
                    stringResource(it)
                }
                val screenLockInSeconds by viewModel.screenLockInSeconds.collectAsStateWithLifecycle()

                var expanded by remember { mutableStateOf(false) }
                val selectedOptionText by remember {
                    derivedStateOf {
                        screenLockSettings[screenLockInSeconds.ordinal]
                    }
                }

                // We want to react on tap/press on TextField to show menu
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        screenLockSettings.forEachIndexed { index, selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    ScreenLockSetting.byPosition(index).let {
                                        viewModel.screenLockInSeconds.value = it
                                    }
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))

            val torEnabled by viewModel.torEnabled.collectAsStateWithLifecycle()
            GreenSwitch(
                title = stringResource(Res.string.id_connect_with_tor),
                caption = stringResource(Res.string.id_private_but_less_stable),
                checked = torEnabled,
                painter = painterResource(Res.drawable.tor),
                onCheckedChange = viewModel.torEnabled.onValueChange()
            )

            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))

            Column {

                val proxyEnabled by viewModel.proxyEnabled.collectAsStateWithLifecycle()
                GreenSwitch(
                    title = stringResource(Res.string.id_connect_through_a_proxy),
                    checked = proxyEnabled,
                    painter = painterResource(Res.drawable.funnel),
                    onCheckedChange = viewModel.proxyEnabled.onValueChange()
                )

                AnimatedVisibility(visible = proxyEnabled) {
                    val proxyUrl by viewModel.proxyUrl.collectAsStateWithLifecycle()
                    OutlinedTextField(
                        value = proxyUrl,
                        onValueChange = viewModel.proxyUrl.onValueChange(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 54.dp, end = 16.dp, bottom = 8.dp),
                        singleLine = true,
                        supportingText = { Text(stringResource(Res.string.id_host_ip)) },
                        isError = proxyUrl.isBlank(),
                        trailingIcon = {
                            TextInputPaste(state = viewModel.proxyUrl)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))

            val rememberHardwareDevices by viewModel.rememberHardwareDevices.collectAsStateWithLifecycle()
            GreenSwitch(
                title = stringResource(Res.string.id_remember_hardware_devices),
                checked = rememberHardwareDevices,
                painter = painterResource(Res.drawable.cpu),
                onCheckedChange = viewModel.rememberHardwareDevices.onValueChange()
            )

            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))

            val testnetEnabled by viewModel.testnetEnabled.collectAsStateWithLifecycle()
            GreenSwitch(
                title = stringResource(Res.string.id_enable_testnet),
                checked = testnetEnabled,
                painter = painterResource(Res.drawable.flask_fill),
                onCheckedChange = viewModel.testnetEnabled.onValueChange()
            )

            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))

            if (viewModel.experimentalFeatureEnabled) {
                val experimentalFeaturesEnabled by viewModel.experimentalFeaturesEnabled.collectAsStateWithLifecycle()
                Column {
                    GreenSwitch(
                        title = stringResource(Res.string.id_enable_experimental_features),
                        caption = stringResource(Res.string.id_experimental_features_might),
                        checked = experimentalFeaturesEnabled,
                        painter = painterResource(Res.drawable.test_tube_fill),
                        onCheckedChange = viewModel.experimentalFeaturesEnabled.onValueChange()
                    )

                }
                HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
            }

            if (viewModel.analyticsFeatureEnabled) {
                val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
                Column {
                    GreenSwitch(
                        title = stringResource(Res.string.id_help_green_improve),
                        caption = stringResource(Res.string.id_enable_limited_usage_data),
                        checked = analyticsEnabled,
                        painter = painterResource(Res.drawable.users_three),
                        onCheckedChange = viewModel.analyticsEnabled.onValueChange()
                    )

                    GreenButton(
                        text = stringResource(Res.string.id_more_info),
                        type = GreenButtonType.TEXT,
                        size = GreenButtonSize.SMALL,
                        modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                    ) {
                        viewModel.postEvent(AppSettingsViewModel.LocalEvents.AnalyticsMoreInfo)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 54.dp))
            }

            Text(
                text = stringResource(Res.string.id_custom_servers_and_validation),
                style = titleLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
            )

            val electrumNodeEnabled by viewModel.electrumNodeEnabled.collectAsStateWithLifecycle()
            GreenSwitch(
                title = stringResource(Res.string.id_personal_electrum_server),
                caption = stringResource(Res.string.id_choose_the_electrum_servers_you),
                checked = electrumNodeEnabled,
                painter = painterResource(Res.drawable.cloud),
                onCheckedChange = viewModel.electrumNodeEnabled.onValueChange()
            )

            AnimatedVisibility(visible = electrumNodeEnabled) {
                GreenColumn(space = 4, padding = 0) {
                    val personalBitcoinElectrumServer by viewModel.personalBitcoinElectrumServer.collectAsStateWithLifecycle()
                    OutlinedTextField(
                        value = personalBitcoinElectrumServer,
                        onValueChange = viewModel.personalBitcoinElectrumServer.onValueChange(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 54.dp, end = 16.dp),
                        singleLine = true,
                        label = { Text(stringResource(Res.string.id_bitcoin_electrum_server)) },
                        placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_BITCOIN_ELECTRUM_URL) },
                        trailingIcon = {
                            TextInputPaste(state = viewModel.personalBitcoinElectrumServer)
                        }
                    )

                    val personalLiquidElectrumServer by viewModel.personalLiquidElectrumServer.collectAsStateWithLifecycle()
                    OutlinedTextField(
                        value = personalLiquidElectrumServer,
                        onValueChange = viewModel.personalLiquidElectrumServer.onValueChange(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 54.dp, end = 16.dp, bottom = 8.dp),
                        singleLine = true,
                        label = { Text(stringResource(Res.string.id_liquid_electrum_server)) },
                        placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_LIQUID_ELECTRUM_URL) },
                        trailingIcon = {
                            TextInputPaste(state = viewModel.personalLiquidElectrumServer)
                        }
                    )

                    if (testnetEnabled) {
                        val personalTestnetElectrumServer by viewModel.personalTestnetElectrumServer.collectAsStateWithLifecycle()
                        OutlinedTextField(
                            value = personalTestnetElectrumServer,
                            onValueChange = viewModel.personalTestnetElectrumServer.onValueChange(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 54.dp, end = 16.dp),
                            singleLine = true,
                            label = { Text(stringResource(Res.string.id_testnet_electrum_server)) },
                            placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_TESTNET_ELECTRUM_URL) },
                            trailingIcon = {
                                TextInputPaste(state = viewModel.personalTestnetElectrumServer)
                            }
                        )

                        val personalTestnetLiquidElectrumServer by viewModel.personalTestnetLiquidElectrumServer.collectAsStateWithLifecycle()
                        OutlinedTextField(
                            value = personalTestnetLiquidElectrumServer,
                            onValueChange = viewModel.personalTestnetLiquidElectrumServer.onValueChange(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 54.dp, end = 16.dp, bottom = 8.dp),
                            singleLine = true,
                            label = { Text(stringResource(Res.string.id_liquid_testnet_electrum_server)) },
                            placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_TESTNET_LIQUID_ELECTRUM_URL) },
                            trailingIcon = {
                                TextInputPaste(state = viewModel.personalTestnetLiquidElectrumServer)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(start = 54.dp))

            val spvEnabled by viewModel.spvEnabled.collectAsStateWithLifecycle()
            GreenSwitch(
                title = stringResource(Res.string.id_spv_verification),
                caption = stringResource(Res.string.id_verify_your_bitcoin),
                checked = spvEnabled,
                painter = painterResource(Res.drawable.shield_check),
                onCheckedChange = viewModel.spvEnabled.onValueChange()
            )

            if (viewModel.multiServerValidationFeatureEnabled) {

                HorizontalDivider(modifier = Modifier.padding(start = 54.dp))

                val multiServerValidationEnabled by viewModel.multiServerValidationEnabled.collectAsStateWithLifecycle()
                GreenSwitch(
                    title = stringResource(Res.string.id_multi_server_validation),
                    caption = stringResource(Res.string.id_double_check_spv_with_other),
                    checked = multiServerValidationEnabled,
                    painter = painterResource(Res.drawable.hard_drives),
                    onCheckedChange = viewModel.multiServerValidationEnabled.onValueChange()
                )

                AnimatedVisibility(visible = multiServerValidationEnabled) {
                    GreenColumn(space = 4, padding = 0) {
                        val spvBitcoinElectrumServer by viewModel.spvBitcoinElectrumServer.collectAsStateWithLifecycle()
                        OutlinedTextField(
                            value = spvBitcoinElectrumServer,
                            onValueChange = viewModel.spvBitcoinElectrumServer.onValueChange(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 54.dp, end = 16.dp),
                            singleLine = true,
                            label = { Text(stringResource(Res.string.id_bitcoin_electrum_server)) },
                            placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_MULTI_SPV_BITCOIN_URL) },
                            trailingIcon = {
                                TextInputPaste(state = viewModel.spvBitcoinElectrumServer)
                            }
                        )

                        val spvLiquidElectrumServer by viewModel.spvLiquidElectrumServer.collectAsStateWithLifecycle()
                        OutlinedTextField(
                            value = spvLiquidElectrumServer,
                            onValueChange = viewModel.spvLiquidElectrumServer.onValueChange(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 54.dp, end = 16.dp, bottom = 8.dp),
                            singleLine = true,
                            label = { Text(stringResource(Res.string.id_liquid_electrum_server)) },
                            placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_MULTI_SPV_LIQUID_URL) },
                            trailingIcon = {
                                TextInputPaste(state = viewModel.spvLiquidElectrumServer)
                            }
                        )

                        if (testnetEnabled) {
                            val spvTestnetElectrumServer by viewModel.spvTestnetElectrumServer.collectAsStateWithLifecycle()
                            OutlinedTextField(
                                value = spvTestnetElectrumServer,
                                onValueChange = viewModel.spvTestnetElectrumServer.onValueChange(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 54.dp, end = 16.dp),
                                singleLine = true,
                                label = { Text(stringResource(Res.string.id_testnet_electrum_server)) },
                                placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_MULTI_SPV_TESTNET_URL) },
                                trailingIcon = {
                                    TextInputPaste(state = viewModel.spvTestnetElectrumServer)
                                }
                            )

                            val spvTestnetLiquidElectrumServer by viewModel.spvTestnetLiquidElectrumServer.collectAsStateWithLifecycle()
                            OutlinedTextField(
                                value = spvTestnetLiquidElectrumServer,
                                onValueChange = viewModel.spvTestnetLiquidElectrumServer.onValueChange(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 54.dp, end = 16.dp, bottom = 8.dp),
                                singleLine = true,
                                label = { Text(stringResource(Res.string.id_liquid_testnet_electrum_server)) },
                                placeholder = { Text(AppSettingsViewModelAbstract.DEFAULT_MULTI_SPV_TESTNET_LIQUID_URL) },
                                trailingIcon = {
                                    TextInputPaste(state = viewModel.spvTestnetLiquidElectrumServer)
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            GreenGradient()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp).padding(bottom = 8.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GreenButton(
                    text = stringResource(Res.string.id_cancel),
                    modifier = Modifier.weight(1f),
                    type = GreenButtonType.TEXT
                ) {
                    viewModel.postEvent(AppSettingsViewModel.LocalEvents.Cancel)
                }

                GreenButton(
                    text = stringResource(Res.string.id_save),
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.postEvent(AppSettingsViewModel.LocalEvents.Save)
                }
            }
        }

    }
}
