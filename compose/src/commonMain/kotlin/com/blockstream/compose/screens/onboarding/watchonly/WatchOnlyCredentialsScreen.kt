package com.blockstream.compose.screens.onboarding.watchonly

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.file
import blockstream_green.common.generated.resources.id_descriptor
import blockstream_green.common.generated.resources.id_import_from_file
import blockstream_green.common.generated.resources.id_log_in
import blockstream_green.common.generated.resources.id_log_in_via_watchonly_to_receive
import blockstream_green.common.generated.resources.id_login
import blockstream_green.common.generated.resources.id_login_with_biometrics
import blockstream_green.common.generated.resources.id_password
import blockstream_green.common.generated.resources.id_remember_me
import blockstream_green.common.generated.resources.id_scan_or_paste_your_extended
import blockstream_green.common.generated.resources.id_scan_or_paste_your_public
import blockstream_green.common.generated.resources.id_username
import blockstream_green.common.generated.resources.id_watchonly_credentials
import blockstream_green.common.generated.resources.id_watchonly_mode_can_be_activated
import blockstream_green.common.generated.resources.id_xpub
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyCredentialsViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyCredentialsViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.LocalBiometricState
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.components.GreenIconButton
import com.blockstream.ui.components.GreenSpacer
import com.blockstream.compose.components.ScanQrButton
import com.blockstream.compose.components.ScreenContainer
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.TextInputPassword
import com.blockstream.compose.utils.TextInputPaste
import com.blockstream.compose.utils.noRippleToggleable
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


@Parcelize
data class WatchOnlyCredentialsScreen(val setupArgs: SetupArgs) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WatchOnlyCredentialsViewModel> {
            parametersOf(setupArgs)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        WatchOnlyCredentialsScreen(viewModel = viewModel)
    }
}

@Composable
fun WatchOnlyCredentialsScreen(
    viewModel: WatchOnlyCredentialsViewModelAbstract
) {
    val biometricsState = LocalBiometricState.current
    val platformManager = rememberPlatformManager()

    CameraBottomSheet.getResult {
        viewModel.postEvent(
            WatchOnlyCredentialsViewModel.LocalEvents.AppendWatchOnlyDescriptor(
                value = it.result
            )
        )
    }

    HandleSideEffect(viewModel = viewModel) {
        if (it is WatchOnlyCredentialsViewModel.LocalSideEffects.RequestCipher) {
            biometricsState?.getBiometricsCipher(viewModel)
        }
    }

    val isSinglesig = viewModel.isSinglesig.value

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()
    ScreenContainer(
        onProgress = onProgress
    ) {
        val focusManager = LocalFocusManager.current

        GreenColumn(
            padding = 0,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
            ) {
                val isOutputDescriptors by viewModel.isOutputDescriptors.collectAsStateWithLifecycle()

                Text(
                    text = stringResource(if (isSinglesig) Res.string.id_watchonly_credentials else Res.string.id_login),
                    style = displayMedium,
                )

                val subtitle = (when {
                    isSinglesig && isOutputDescriptors -> Res.string.id_scan_or_paste_your_public
                    isSinglesig -> Res.string.id_scan_or_paste_your_extended
                    else -> Res.string.id_log_in_via_watchonly_to_receive
                })

                AnimatedContent(targetState = subtitle, transitionSpec = {
                    // Compare the incoming number with the previous number.
                    fadeIn().togetherWith(fadeOut())

                }) { res ->
                    Text(
                        text = stringResource(res),
                        style = bodyLarge
                    )
                }

                GreenColumn(
                    padding = 0, modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val isLiquid by viewModel.isLiquid.collectAsStateWithLifecycle()
                    val isOutputDescriptors by viewModel.isOutputDescriptors.collectAsStateWithLifecycle()

                    if (isSinglesig) {
                        val watchOnlyDescriptor by viewModel.watchOnlyDescriptor.collectAsStateWithLifecycle()

                        if (!isLiquid) {
                            val options = listOf(
                                stringResource(Res.string.id_xpub),
                                stringResource(Res.string.id_descriptor)
                            )

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                options.forEachIndexed { index, label ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = options.size
                                        ),
                                        onClick = {
                                            viewModel.isOutputDescriptors.value = index == 1
                                        },
                                        selected = index == if (isOutputDescriptors) 1 else 0
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                        }

                        Column {
                            TextField(
                                value = watchOnlyDescriptor,
                                onValueChange = viewModel.watchOnlyDescriptor.onValueChange(),
                                minLines = 5,
                                maxLines = 5,
                                placeholder = {
                                    Text(if (isOutputDescriptors) "Descriptor1,Descriptor2,…" else "xPub1,yPub1,zPub1,xPub2,…")
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
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

                                    FilePicker(show = showFilePicker, fileExtensions = listOf("json")) { platformFile ->
                                        showFilePicker = false
                                        platformFile?.path?.let { platformManager.fileToSource(it) }?.also {
                                            viewModel.postEvent(
                                                WatchOnlyCredentialsViewModel.LocalEvents.ImportFile(it)
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

                                val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
                                ScanQrButton {
                                    bottomSheetNavigator?.show(
                                        CameraBottomSheet(
                                            isDecodeContinuous = true,
                                            parentScreenName = viewModel.screenName(),
                                            setupArgs = viewModel.setupArgs
                                        )
                                    )
                                }
                            }
                        }


                    } else {

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
                    }

                    Column {
                        val isRememberMe by viewModel.isRememberMe.collectAsStateWithLifecycle()
                        if (!isSinglesig) {
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

                        val canUseBiometrics by viewModel.canUseBiometrics.collectAsStateWithLifecycle()
                        val withBiometrics by viewModel.withBiometrics.collectAsStateWithLifecycle()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.noRippleToggleable(
                                value = withBiometrics,
                                enabled = isRememberMe && canUseBiometrics,
                                onValueChange = viewModel.withBiometrics.onValueChange()
                            )
                        ) {
                            Text(
                                text = stringResource(Res.string.id_login_with_biometrics),
                                style = bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f),
                                color = if (isRememberMe && canUseBiometrics) whiteHigh else whiteLow
                            )

                            Switch(
                                checked = withBiometrics,
                                onCheckedChange = viewModel.withBiometrics.onValueChange(),
                                enabled = isRememberMe && canUseBiometrics
                            )
                        }
                    }

                    val isLoginEnabled by viewModel.isLoginEnabled.collectAsStateWithLifecycle()
                    Column {
                        GreenButton(
                            text = stringResource(Res.string.id_log_in),
                            size = GreenButtonSize.BIG,
                            enabled = isLoginEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            focusManager.clearFocus()
                            viewModel.postEvent(Events.Continue)
                        }

                        if (!isSinglesig) {
                            Text(
                                text = stringResource(Res.string.id_watchonly_mode_can_be_activated),
                                style = bodyMedium,
                                color = whiteMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            AppSettingsButton(
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                viewModel.postEvent(NavigateDestinations.AppSettings)
            }
        }
    }
}