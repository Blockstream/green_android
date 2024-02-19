package com.blockstream.compose.screens.onboarding.watchonly

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyCredentialsViewModel
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyCredentialsViewModelAbstract
import com.blockstream.common.models.onboarding.watchonly.WatchOnlyCredentialsViewModelPreview
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.IconButton
import com.blockstream.compose.components.ScanQrButton
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sideeffects.BiometricsState
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
import com.blockstream.compose.views.ScreenContainer
import okio.source
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Parcelize
data class WatchOnlyCredentialsScreen(val setupArgs: SetupArgs) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<WatchOnlyCredentialsViewModelAbstract> {
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
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val dialog = LocalDialog.current
    val androidKeystore: AndroidKeystore =
        if (LocalInspectionMode.current) AndroidKeystore(context) else koinInject()

    val biometricsState = remember {
        BiometricsState(
            context = context,
            coroutineScope = scope,
            snackbarHostState = snackbar,
            dialogState = dialog,
            androidKeystore = androidKeystore
        )
    }

    getNavigationResult<ScanResult>(CameraBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(
            WatchOnlyCredentialsViewModel.LocalEvents.AppendWatchOnlyDescriptor(
                value = it.result
            )
        )
    }

    HandleSideEffect(viewModel = viewModel) {
        if (it is WatchOnlyCredentialsViewModel.LocalSideEffects.RequestCipher) {
            biometricsState.getBiometricsCipher(viewModel)
        }
    }

    val isSinglesig = viewModel.isSinglesig.value

    ScreenContainer(
        viewModel = viewModel
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
                    text = stringResource(if (isSinglesig) R.string.id_watchonly_credentials else R.string.id_login),
                    style = displayMedium,
                )

                val subtitle = (when {
                    isSinglesig && isOutputDescriptors -> R.string.id_scan_or_paste_your_public
                    isSinglesig -> R.string.id_scan_or_paste_your_extended
                    else -> R.string.id_log_in_via_watchonly_to_receive
                })

                AnimatedContent(targetState = subtitle, transitionSpec = {
                    // Compare the incoming number with the previous number.
                    fadeIn().togetherWith(fadeOut())

                }) { res ->
                    Text(
                        text = stringResource(id = res),
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
                                stringResource(id = R.string.id_xpub),
                                stringResource(id = R.string.id_descriptor)
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
                                    val openDocument =
                                        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                                            uri?.also {
                                                context.contentResolver.openInputStream(uri)
                                                    ?.source()?.also {
                                                        viewModel.postEvent(
                                                            WatchOnlyCredentialsViewModel.LocalEvents.ImportFile(
                                                                it
                                                            )
                                                        )
                                                    }
                                            }
                                        }

                                    IconButton(
                                        text = stringResource(id = R.string.id_import_from_file),
                                        icon = painterResource(id = R.drawable.file)
                                    ) {
                                        openDocument.launch(arrayOf("application/json"))
                                    }
                                } else {
                                    GreenSpacer(space = 0)
                                }

                                val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
                                ScanQrButton {
                                    bottomSheetNavigator.show(
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
                                autoCorrect = false,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            label = { Text(stringResource(id = R.string.id_username)) },
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
                                autoCorrect = false,
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            label = { Text(stringResource(id = R.string.id_password)) },

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
                                    text = stringResource(id = R.string.id_remember_me),
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
                                text = stringResource(id = R.string.id_login_with_biometrics),
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
                            text = stringResource(id = R.string.id_log_in),
                            size = GreenButtonSize.BIG,
                            enabled = isLoginEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            focusManager.clearFocus()
                            viewModel.postEvent(Events.Continue)
                        }

                        if (!isSinglesig) {
                            Text(
                                text = stringResource(id = R.string.id_watchonly_mode_can_be_activated),
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
                viewModel.postEvent(Events.AppSettings)
            }
        }
    }
}

@Composable
@Preview
fun WatchOnlyCredentialSinglesigPreview() {
    GreenPreview {
        WatchOnlyCredentialsScreen(
            viewModel = WatchOnlyCredentialsViewModelPreview.preview(
                isSinglesig = true
            )
        )
    }
}

@Composable
@Preview
fun WatchOnlyCredentialsSinglesigLiquidPreview() {
    GreenPreview {
        WatchOnlyCredentialsScreen(
            viewModel = WatchOnlyCredentialsViewModelPreview.preview(
                isSinglesig = true,
                isLiquid = true
            )
        )
    }
}