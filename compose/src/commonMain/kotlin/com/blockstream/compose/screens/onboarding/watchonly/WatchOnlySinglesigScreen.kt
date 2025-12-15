package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.file
import blockstream_green.common.generated.resources.id_import_from_file
import blockstream_green.common.generated.resources.id_import_wallet
import blockstream_green.common.generated.resources.id_in_a_watch_only_wallet_your_private_keys_remain
import blockstream_green.common.generated.resources.id_paste_descriptors_placeholder
import blockstream_green.common.generated.resources.id_scan_or_paste_xpub_descriptor
import blockstream_green.common.generated.resources.id_set_up_watchonly_wallet
import blockstream_green.common.generated.resources.id_set_up_with_username_and_password
import com.blockstream.data.data.ScanResult
import com.blockstream.compose.LocalBiometricState
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenIconButton
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.ScanQrButton
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.models.onboarding.watchonly.WatchOnlySinglesigViewModel
import com.blockstream.compose.models.onboarding.watchonly.WatchOnlySinglesigViewModelAbstract
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.sheets.WatchOnlyNetworkBottomSheet
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.TextInputPaste
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchOnlySinglesigScreen(
    viewModel: WatchOnlySinglesigViewModelAbstract
) {
    val biometricsState = LocalBiometricState.current
    val platformManager = rememberPlatformManager()

    NavigateDestinations.Camera.getResult<ScanResult> {
        viewModel.postEvent(
            WatchOnlySinglesigViewModel.LocalEvents.AppendWatchOnlyDescriptor(
                value = it.result
            )
        )
    }

    var showWatchOnlyNetworkBottomSheet by remember { mutableStateOf(false) }

    if (showWatchOnlyNetworkBottomSheet) {
        WatchOnlyNetworkBottomSheet(
            viewModel = viewModel,
            setupArgs = viewModel.setupArgs,
            onDismissRequest = { showWatchOnlyNetworkBottomSheet = false }
        )
    }

    SetupScreen(viewModel, withPadding = false, sideEffectsHandler = {
        if (it is WatchOnlySinglesigViewModel.LocalSideEffects.RequestCipher) {
            biometricsState?.getBiometricsCipher(viewModel)
        }
    }) {
        GreenColumn(
            padding = 0,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            val focusManager = LocalFocusManager.current

            GreenColumn(
                padding = 0,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(Res.string.id_set_up_watchonly_wallet),
                    style = displayMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(Res.string.id_in_a_watch_only_wallet_your_private_keys_remain),
                    style = bodyLarge,
                    color = whiteMedium
                )

                Text(
                    text = stringResource(Res.string.id_scan_or_paste_xpub_descriptor),
                    style = bodyLarge,
                    color = whiteMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                GreenColumn(
                    padding = 0,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val isLiquid by viewModel.isLiquid.collectAsStateWithLifecycle()
                    val watchOnlyDescriptor by viewModel.watchOnlyDescriptor.collectAsStateWithLifecycle()

                    Column {
                        TextField(
                            value = watchOnlyDescriptor,
                            onValueChange = viewModel.watchOnlyDescriptor.onValueChange(),
                            placeholder = {
                                Text(
                                    text = stringResource(Res.string.id_paste_descriptors_placeholder),
                                    color = whiteMedium
                                )
                            },
                            minLines = 5,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .align(Alignment.End)
                                ) {
                                    TextInputPaste(viewModel.watchOnlyDescriptor)
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!isLiquid) {
                                var showFilePicker by remember { mutableStateOf(false) }

                                FilePicker(
                                    show = showFilePicker,
                                    fileExtensions = listOf("json")
                                ) { platformFile ->
                                    showFilePicker = false
                                    platformFile?.path?.let {
                                        platformManager.fileToSource(it)
                                    }?.also {
                                        viewModel.postEvent(
                                            WatchOnlySinglesigViewModel.LocalEvents.ImportFile(it)
                                        )
                                    }
                                }

                                GreenIconButton(
                                    text = stringResource(Res.string.id_import_from_file),
                                    icon = painterResource(Res.drawable.file)
                                ) {
                                    showFilePicker = true
                                }
                            } else {
                                GreenSpacer(space = 0)
                            }

                            ScanQrButton {
                                viewModel.postEvent(
                                    NavigateDestinations.Camera(
                                        isDecodeContinuous = true,
                                        parentScreenName = viewModel.screenName(),
                                        setupArgs = viewModel.setupArgs
                                    )
                                )
                            }
                        }
                    }

                    // Import Wallet button
                    val isLoginEnabled by viewModel.isLoginEnabled.collectAsStateWithLifecycle()
                    Column {
                        GreenButton(
                            text = stringResource(Res.string.id_import_wallet),
                            size = GreenButtonSize.BIG,
                            enabled = isLoginEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            focusManager.clearFocus()
                            viewModel.postEvent(Events.Continue)
                        }

                        GreenButton(
                            text = stringResource(Res.string.id_set_up_with_username_and_password),
                            type = GreenButtonType.TEXT,
                            size = GreenButtonSize.BIG,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            showWatchOnlyNetworkBottomSheet = true
                        }
                    }
                }
            }
        }
    }
}