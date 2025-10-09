package com.blockstream.compose.screens.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_biometrics
import blockstream_green.common.generated.resources.id_compare_security_levels
import blockstream_green.common.generated.resources.id_connect_hardware_wallet
import blockstream_green.common.generated.resources.id_firmware_update
import blockstream_green.common.generated.resources.id_genuine_check
import blockstream_green.common.generated.resources.id_hardware
import blockstream_green.common.generated.resources.id_mobile
import blockstream_green.common.generated.resources.id_pin
import blockstream_green.common.generated.resources.id_recovery
import blockstream_green.common.generated.resources.id_recovery_phrase
import blockstream_green.common.generated.resources.id_security_level_
import blockstream_green.common.generated.resources.id_unlock_method
import blockstream_green.common.generated.resources.id_watchonly
import blockstream_green.common.generated.resources.id_your_device
import blockstream_green.common.generated.resources.id_your_jade
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Binoculars
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.Cpu
import com.adamglin.phosphoricons.regular.Fingerprint
import com.adamglin.phosphoricons.regular.Key
import com.adamglin.phosphoricons.regular.Password
import com.adamglin.phosphoricons.regular.PlugsConnected
import com.adamglin.phosphoricons.regular.SealCheck
import com.adamglin.phosphoricons.regular.ShieldChevron
import com.blockstream.common.Urls
import com.blockstream.common.data.AlertType
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.MenuEntry
import com.blockstream.common.data.MenuEntryList
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.overview.SecurityViewModel
import com.blockstream.common.models.overview.SecurityViewModel.LocalSideEffects
import com.blockstream.common.models.overview.SecurityViewModelAbstract
import com.blockstream.common.models.overview.SecurityViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAlert
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.ListHeader
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.components.Promo
import com.blockstream.compose.screens.overview.components.WatchOnlyWalletDescription
import com.blockstream.compose.theme.displaySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.navigation.LocalInnerPadding
import com.blockstream.ui.navigation.getResult
import com.blockstream.ui.utils.bottom
import com.blockstream.ui.utils.plus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SecurityScreen(viewModel: SecurityViewModelAbstract) {

    var channels by remember { mutableStateOf<List<String>?>(null) }

    NavigateDestinations.Menu.getResult<Int> {
        channels?.getOrNull(it)?.also { channel ->
            viewModel.firmwareUpdate(channel = channel)
        }
    }

    NavigateDestinations.DeviceScan.getResult<GreenWallet> {
        viewModel.executePendingAction()
    }

    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val showRecoveryConfirmation by viewModel.showRecoveryConfirmation.collectAsStateWithLifecycle()

    val innerPadding = LocalInnerPadding.current
    val listState = rememberLazyListState()

    val isHardware = viewModel.isHardware
    val isJade by viewModel.isJade.collectAsStateWithLifecycle()

    val isWatchOnly by viewModel.isWatchOnly.collectAsStateWithLifecycle()
    val isQrWatchOnly by viewModel.isQrWatchOnly.collectAsStateWithLifecycle()
    val showGenuineCheck by viewModel.showGenuineCheck.collectAsStateWithLifecycle()

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        withBottomInsets = false,
        onProgressStyle = OnProgressStyle.Full(),
        sideEffectsHandler = {
            if (it is LocalSideEffects.SelectFirmwareChannel) {
                channels = it.channels
                viewModel.postEvent(
                    NavigateDestinations.Menu(
                        title = "Select Firmware Channel",
                        entries = MenuEntryList(it.channels.map {
                            MenuEntry(
                                title = it,
                            )
                        })
                    )
                )
            }
        }) {

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = innerPadding
                .bottom()
                .plus(PaddingValues(horizontal = 16.dp))
                .plus(PaddingValues(bottom = 80.dp + 24.dp))
        ) {

            if (!isWatchOnly) {
                item {
                    GreenColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ShieldChevron,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(
                                Res.string.id_security_level_,
                                if (isHardware) "2" else "1"
                            ), color = whiteMedium
                        )
                        Text(
                            text = stringResource(if (isHardware) Res.string.id_hardware else Res.string.id_mobile),
                            style = displaySmall
                        )

                        if (!isHardware) {
                            GreenButton(
                                text = stringResource(Res.string.id_compare_security_levels),
                                type = GreenButtonType.OUTLINE,
                                size = GreenButtonSize.BIG,
                                color = GreenButtonColor.GREENER
                            ) {
                                viewModel.postEvent(NavigateDestinations.SecurityLevel(greenWallet = viewModel.greenWallet))
                            }
                        }
                    }
                }
            }

            if (isHardware && !isQrWatchOnly) {

                if (isJade) {
                    item(key = "jade_header") {
                        ListHeader(title = stringResource(Res.string.id_your_jade))
                    }
                } else {
                    item(key = "device_header") {
                        ListHeader(title = stringResource(Res.string.id_your_device))
                    }
                }

//                if (isHwWatchOnly) {
//                    item {
//                        SecurityItem(
//                            title = stringResource(Res.string.id_connect_hardware_wallet),
//                            icon = PhosphorIcons.Regular.PlugsConnected,
//                            state = null,
//                        ) {
//                            viewModel.postEvent(
//                                NavigateDestinations.DeviceScan(
//                                    greenWallet = viewModel.greenWallet,
//                                    isWatchOnlyUpgrade = true
//                                )
//                            )
//                        }
//                    }
//                }

                if (isJade) {
                    if (showGenuineCheck) {
                        item {
                            SecurityItem(
                                title = stringResource(Res.string.id_genuine_check),
                                icon = PhosphorIcons.Regular.SealCheck,
                                state = null,
                            ) {
                                viewModel.genuineCheck()
                            }
                        }
                    }

                    item {
                        SecurityItem(
                            title = stringResource(Res.string.id_firmware_update),
                            icon = PhosphorIcons.Regular.Cpu,
                            state = null,
                        ) {
                            viewModel.firmwareUpdate()
                        }
                    }
                } else {
                    item {
                        SecurityItem(
                            title = stringResource(Res.string.id_connect_hardware_wallet),
                            icon = PhosphorIcons.Regular.PlugsConnected,
                            state = null,
                        ) {
                            viewModel.connectDevice()
                        }
                    }
                }

            } else if (isWatchOnly) {
                item {
                    GreenColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Binoculars,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Text(
                            text = stringResource(Res.string.id_watchonly),
                            style = titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        WatchOnlyWalletDescription { viewModel.postEvent(Events.OpenBrowser(Urls.SECURITY_WATCH_ONLY)) }
                    }
                }
            } else {

                if (showRecoveryConfirmation) {
                    item {
                        GreenAlert(
                            alertType = AlertType.RecoveryIsUnconfirmed(withCloseButton = false),
                            viewModel = viewModel,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                item(key = "unlock_method") {
                    ListHeader(title = stringResource(Res.string.id_unlock_method))
                }

                items(credentials) { credential ->
                    SecurityItem(
                        title = when (credential.first) {
                            CredentialType.PIN_PINDATA -> stringResource(Res.string.id_pin)
                            CredentialType.BIOMETRICS_MNEMONIC -> stringResource(Res.string.id_biometrics)
                            else -> ""
                        },
                        icon = if (credential.first == CredentialType.PIN_PINDATA) PhosphorIcons.Regular.Password else PhosphorIcons.Regular.Fingerprint,
                        state = credential.second != null,
                    ) {
                        if (credential.first == CredentialType.PIN_PINDATA) {
                            if (credential.second == null) {
                                viewModel.postEvent(SecurityViewModel.LocalEvents.EnablePin)
                            } else {
                                viewModel.postEvent(SecurityViewModel.LocalEvents.DisablePin)
                            }
                        } else {
                            if (credential.second == null) {
                                viewModel.postEvent(SecurityViewModel.LocalEvents.EnableBiometrics)
                            } else {
                                viewModel.postEvent(SecurityViewModel.LocalEvents.DisableBiometrics)
                            }
                        }
                    }
                }

                if (!viewModel.greenWallet.isHardware) {
                    item(key = "recovery") {
                        ListHeader(title = stringResource(Res.string.id_recovery))
                    }

                    item {
                        SecurityItem(
                            title = stringResource(Res.string.id_recovery_phrase),
                            icon = PhosphorIcons.Regular.Key,
                            state = null
                        ) {
                            viewModel.postEvent(
                                NavigateDestinations.RecoveryIntro(
                                    setupArgs = SetupArgs(
                                        greenWallet = viewModel.greenWallet,
                                        isShowRecovery = true
                                    )
                                )
                            )
                        }
                    }
                }


                item(key = "Promo") {
                    Promo(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityItem(title: String, icon: ImageVector, state: Boolean?, onClick: () -> Unit) {
    GreenCard(onClick = {
        onClick()
    }, padding = 0) {
        GreenRow(padding = 0, modifier = Modifier.height(70.dp).padding(horizontal = 16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = whiteMedium,
            )
            Text(
                text = title,
                style = titleSmall,
                modifier = Modifier.weight(1f)
            )

            state?.also {
                Text(
                    text = if (it) "ON" else "OFF",
                    color = if (it) green else whiteMedium,
                    style = labelLarge
                )
            }

            Icon(
                imageVector = PhosphorIcons.Regular.CaretRight,
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
fun PreviewSecurityScreenCommon() {
    GreenPreview {
        SecurityScreen(viewModel = SecurityViewModelPreview.preview(isHardware = true))
    }
}