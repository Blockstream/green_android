package com.blockstream.compose.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.bip39_passphrase
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.eye_slash
import blockstream_green.common.generated.resources.id_authenticate
import blockstream_green.common.generated.resources.id_bip39_passphrase_login
import blockstream_green.common.generated.resources.id_connect_hardware_wallet
import blockstream_green.common.generated.resources.id_connecting_through_tor
import blockstream_green.common.generated.resources.id_emergency_recovery_phrase
import blockstream_green.common.generated.resources.id_enter_your_pin
import blockstream_green.common.generated.resources.id_log_in
import blockstream_green.common.generated.resources.id_log_in_via_watchonly_to_receive
import blockstream_green.common.generated.resources.id_logging_in
import blockstream_green.common.generated.resources.id_password
import blockstream_green.common.generated.resources.id_pin
import blockstream_green.common.generated.resources.id_restore_with_recovery_phrase
import blockstream_green.common.generated.resources.id_too_many_pin_attempts
import blockstream_green.common.generated.resources.id_username
import blockstream_green.common.generated.resources.id_youve_entered_an_invalid_pin
import blockstream_green.common.generated.resources.qr_code
import blockstream_green.common.generated.resources.shield_warning
import blockstream_green.common.generated.resources.tor
import blockstream_green.common.generated.resources.x
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Fingerprint
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.managers.LifecycleManager
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalBiometricState
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.BiometricsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.components.RichWatchOnlyButton
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.models.login.LoginViewModel
import com.blockstream.compose.models.login.LoginViewModelAbstract
import com.blockstream.compose.models.login.LoginViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.headlineMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AlphaPulse
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.compose.utils.TextInputPassword
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.views.PinView
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun LoginScreen(
    viewModel: LoginViewModelAbstract,
) {
    val biometricsState = LocalBiometricState.current

    val pinCredentials by viewModel.pinCredentials.collectAsStateWithLifecycle()
    val passwordCredentials by viewModel.passwordCredentials.collectAsStateWithLifecycle()
    val biometricsCredentials by viewModel.biometricsCredentials.collectAsStateWithLifecycle()
    val mnemonicCredentials by viewModel.mnemonicCredentials.collectAsStateWithLifecycle()
    val hwWatchOnlyCredentials by viewModel.hwWatchOnlyCredentials.collectAsStateWithLifecycle()


    NavigateDestinations.Bip39Passphrase.getResult<String> {
        viewModel.postEvent(
            LoginViewModel.LocalEvents.Bip39Passphrase(it, null)
        )
    }

    // Device Passphrase
    NavigateDestinations.DevicePassphrase.getResult<String> {
        viewModel.postEvent(Events.DeviceRequestResponse(it))
    }

    // Device PinMatrix
    NavigateDestinations.DevicePin.getResult<String> {
        viewModel.postEvent(Events.DeviceRequestResponse(it))
    }

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

    val lifecycleManager: LifecycleManager = koinInject()

    SetupScreen(viewModel = viewModel, sideEffectsHandler = {
        when (it) {
            is LoginViewModel.LocalSideEffects.LaunchBiometrics -> {
                lifecycleManager.lifecycleState.filter { it.isForeground() }.first()
                biometricsState?.launchBiometricPrompt(it.loginCredentials, viewModel = viewModel)
            }

            is LoginViewModel.LocalSideEffects.LaunchUserPresence -> {
                biometricsState?.launchUserPresencePrompt(title = getString(Res.string.id_authenticate)) {
                    if (it != false) {
                        viewModel.postEvent(LoginViewModel.LocalEvents.Authenticated(it == true))
                    }
                }
            }

            is SideEffects.WalletDelete -> {
                viewModel.postEvent(NavigateDestinations.Home)
            }

            is SideEffects.NavigateBack -> {
                NavigateDestinations.Login.setResult(viewModel.greenWallet)
            }
        }
    }, withPadding = false, onProgressStyle = OnProgressStyle.Disabled) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {

            if (onProgress) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val applicationSettings by viewModel.applicationSettings.collectAsStateWithLifecycle()
                    val tor by viewModel.tor.collectAsStateWithLifecycle()

                    val isLogging = tor.progress == 100 || !applicationSettings.tor
                    Box {
                        if (isLogging) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(120.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = {
                                    tor.progress.toFloat()
                                },
                                modifier = Modifier
                                    .size(120.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }

                        viewModel.deviceOrNull?.also {
                            Image(
                                painter = painterResource(it.icon()),
                                contentDescription = it.deviceBrand.toString(),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(72.dp)
                            )
                        }
                    }

                    GreenSpacer(32)

                    Text(
                        text = stringResource(if (isLogging) Res.string.id_logging_in else Res.string.id_connecting_through_tor),
                        style = titleLarge,
                    )

                    if (applicationSettings.tor) {
                        GreenSpacer(16)
                        AlphaPulse {
                            Image(
                                painter = painterResource(Res.drawable.tor),
                                contentDescription = "Tor"
                            )
                        }
                    }

                    applicationSettings.proxyUrl.takeIf { it.isNotBlank() }?.also {
                        GreenSpacer(4)
                        Text(
                            text = applicationSettings.proxyUrl ?: "proxy Url",
                            style = bodyMedium,
                            color = whiteMedium
                        )
                    }
                }
            }

            val showRestoreWithRecovery by viewModel.showRestoreWithRecovery.collectAsStateWithLifecycle()
            if (showRestoreWithRecovery) {
                GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(Res.string.id_too_many_pin_attempts), style = titleSmall)

                    Image(
                        painter = painterResource(Res.drawable.shield_warning),
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp)
                            .aspectRatio(1f)
                    )

                    GreenSpacer()

                    Text(
                        text = stringResource(Res.string.id_youve_entered_an_invalid_pin),
                        style = labelMedium
                    )

                    GreenButton(text = stringResource(Res.string.id_restore_with_recovery_phrase)) {
                        viewModel.postEvent(LoginViewModel.LocalEvents.ClickRestoreWithRecovery)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                if (!onProgress) {
                    Banner(viewModel)
                }

                if (viewModel.greenWallet.isWatchOnly || hwWatchOnlyCredentials.isSuccess()) {
                    if (!onProgress) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {

                            val isHwWatchOnly = hwWatchOnlyCredentials.isSuccess()

                            GreenColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
                                horizontalAlignment = Alignment.End
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterHorizontally)
                                ) {

                                    if (isHwWatchOnly) {
                                        Image(
                                            painter = painterResource(viewModel.greenWallet.deviceIdentifiers.icon()),
                                            contentDescription = "",
                                            modifier = Modifier
                                                .size(128.dp)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(if (viewModel.greenWallet.isWatchOnlyQr) Res.drawable.qr_code else Res.drawable.eye),
                                            contentDescription = "Watch Only",
                                            // colorFilter = ColorFilter.tint(green),
                                            alpha = 0.25f,
                                            modifier = Modifier
                                                .size(128.dp)
                                        )
                                    }

                                }

                                Text(
                                    text = stringResource(Res.string.id_log_in_via_watchonly_to_receive),
                                    style = titleLarge
                                )

                                val showWatchOnlyUsername by viewModel.showWatchOnlyUsername.collectAsStateWithLifecycle()
                                if (showWatchOnlyUsername) {
                                    val watchOnlyUsername by viewModel.watchOnlyUsername.collectAsStateWithLifecycle()
                                    TextField(
                                        value = watchOnlyUsername,
                                        onValueChange = viewModel.watchOnlyUsername.onValueChange(),
                                        enabled = false,
                                        label = { Text(stringResource(Res.string.id_username)) },
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        singleLine = true,
                                    )
                                }

                                val showWatchOnlyPassword by viewModel.showWatchOnlyPassword.collectAsStateWithLifecycle()
                                val focusManager = LocalFocusManager.current
                                if (showWatchOnlyPassword) {
                                    val watchOnlyPassword by viewModel.watchOnlyPassword.collectAsStateWithLifecycle()
                                    var passwordVisibility: Boolean by remember { mutableStateOf(false) }
                                    TextField(
                                        value = watchOnlyPassword,
                                        onValueChange = viewModel.watchOnlyPassword.onValueChange(),
                                        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                                        label = { Text(stringResource(Res.string.id_password)) },
                                        modifier = Modifier
                                            .fillMaxWidth(),
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
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                passwordVisibility = !passwordVisibility
                                            }) {
                                                Icon(
                                                    painter = painterResource(if (passwordVisibility) Res.drawable.eye_slash else Res.drawable.eye),
                                                    contentDescription = "password visibility",
                                                )
                                            }
                                        }
                                    )
                                }

                                val isWatchOnlyLoginEnabled by viewModel.isWatchOnlyLoginEnabled.collectAsStateWithLifecycle()
                                GreenButton(
                                    text = stringResource(Res.string.id_log_in),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = isWatchOnlyLoginEnabled
                                ) {
                                    viewModel.postEvent(LoginViewModel.LocalEvents.LoginWatchOnly)
                                }

                                if (hwWatchOnlyCredentials.isSuccess()) {
                                    GreenButton(
                                        text = stringResource(Res.string.id_connect_hardware_wallet),
                                        type = GreenButtonType.TEXT,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        viewModel.postEvent(
                                            NavigateDestinations.DeviceScan(
                                                greenWallet = viewModel.greenWallet
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {

                        if ((pinCredentials.isNotEmpty() || passwordCredentials.isNotEmpty()) && !onProgress) {
                            ConstraintLayout(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {

                                val (titleRef, containerRef) = createRefs()

                                Text(
                                    text = stringResource(Res.string.id_enter_your_pin),
                                    style = headlineMedium,
                                    modifier = Modifier.constrainAs(titleRef) {
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                        top.linkTo(parent.top)
                                        bottom.linkTo(parent.bottom)
                                    }
                                )

                                GreenColumn(
                                    padding = 0,
                                    space = 8,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.constrainAs(containerRef) {
                                        top.linkTo(titleRef.bottom)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                    }) {

                                    val isEmergencyRecoveryPhrase by viewModel.isEmergencyRecoveryPhrase.collectAsStateWithLifecycle()

                                    if (isEmergencyRecoveryPhrase) {
                                        OutlinedButton(onClick = {
                                            viewModel.postEvent(
                                                LoginViewModel.LocalEvents.EmergencyRecovery(
                                                    false
                                                )
                                            )
                                        }) {
                                            GreenRow(padding = 0, space = 6) {
                                                Text(
                                                    text = stringResource(Res.string.id_emergency_recovery_phrase),
                                                    style = labelLarge,
                                                    color = whiteMedium
                                                )

                                                Icon(
                                                    painter = painterResource(Res.drawable.x),
                                                    contentDescription = null,
                                                    tint = whiteMedium,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    val bip39Passphrase by viewModel.bip39Passphrase.collectAsStateWithLifecycle()

                                    if (bip39Passphrase.isNotBlank()) {
                                        TextButton(onClick = {
                                            viewModel.postEvent(
                                                NavigateDestinations.Bip39Passphrase(
                                                    greenWallet = viewModel.greenWallet,
                                                    passphrase = viewModel.bip39Passphrase.value
                                                )
                                            )
                                        }) {
                                            GreenRow(padding = 0, space = 6) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.bip39_passphrase),
                                                    contentDescription = null,
                                                    tint = whiteMedium,
                                                    modifier = Modifier.size(16.dp)
                                                )

                                                Text(
                                                    text = stringResource(Res.string.id_bip39_passphrase_login),
                                                    style = labelLarge,
                                                    color = whiteMedium
                                                )
                                            }
                                        }
                                    }
                                }

                            }
                        }

                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .weight(4f),
                        contentAlignment = Alignment.Center
                    ) {

                        val error by viewModel.error.collectAsStateWithLifecycle()
                        if (!onProgress) {
                            if (pinCredentials.isNotEmpty()) {
                                PinView(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .align(Alignment.BottomCenter),
                                    error = error,
                                    onPin = {
                                        if (it.isNotBlank()) {
                                            viewModel.postEvent(
                                                LoginViewModel.LocalEvents.LoginWithPin(
                                                    it
                                                )
                                            )
                                        }
                                    }
                                )
                            } else if (passwordCredentials.isNotEmpty()) {
                                val focusManager = LocalFocusManager.current
                                var password by remember {
                                    mutableStateOf("")
                                }
                                val passwordVisibility = remember { mutableStateOf(false) }

                                GreenColumn(modifier = Modifier.align(Alignment.TopCenter)) {
                                    TextField(
                                        value = password,
                                        visualTransformation = if (passwordVisibility.value) VisualTransformation.None else PasswordVisualTransformation(),
                                        onValueChange = {
                                            password = it
                                        },
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
                                        label = { Text(stringResource(Res.string.id_pin)) },
                                        trailingIcon = {
                                            TextInputPassword(passwordVisibility)
                                        }
                                    )

                                    AnimatedNullableVisibility(value = error) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = it,
                                            style = labelMedium,
                                            color = red
                                        )
                                    }

                                    GreenButton(
                                        text = stringResource(Res.string.id_log_in),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        viewModel.postEvent(
                                            LoginViewModel.LocalEvents.LoginWithPin(password)
                                        )
                                    }
                                }
                            } else if (biometricsCredentials.isNotEmpty()) {

                                Box(modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Fingerprint,
                                        contentDescription = "Fingerprint",
                                        modifier = Modifier
                                            .size(128.dp)
                                            .align(Alignment.Center)
                                            .noRippleClickable {
                                                viewModel.postEvent(LoginViewModel.LocalEvents.ClickBiometrics)
                                            }
                                    )
                                }
                            } else if (mnemonicCredentials.isNotEmpty()) {

                                Image(
                                    painter = painterResource(if (viewModel.greenWallet.isWatchOnlyQr) Res.drawable.qr_code else Res.drawable.eye),
                                    contentDescription = "Watch Only",
                                    // colorFilter = ColorFilter.tint(green),
                                    alpha = 0.25f,
                                    modifier = Modifier
                                        .size(128.dp)
                                )

                                GreenColumn(modifier = Modifier.align(Alignment.BottomCenter)) {
                                    GreenButton(
                                        text = stringResource(Res.string.id_log_in),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        viewModel.postEvent(
                                            LoginViewModel.LocalEvents.Login
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    if (biometricsCredentials.isNotEmpty() && !onProgress) {
                        BiometricsButton(modifier = Modifier.align(Alignment.CenterStart)) {
                            viewModel.postEvent(LoginViewModel.LocalEvents.ClickBiometrics)
                        }
                    }

                    val richWatchOnlyCredentials by viewModel.richWatchOnlyCredentials.collectAsStateWithLifecycle()

                    if (!onProgress && richWatchOnlyCredentials.isNotEmpty()) {
                        RichWatchOnlyButton(modifier = Modifier.align(Alignment.Center)) {
                            viewModel.postEvent(LoginViewModel.LocalEvents.LoginWatchOnly)
                        }
                    }

                    if (!onProgress) {
                        AppSettingsButton(modifier = Modifier.align(Alignment.CenterEnd)) {
                            viewModel.postEvent(NavigateDestinations.AppSettings)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun LoginScreenPreview() {
    GreenPreview {
        LoginScreen(viewModel = LoginViewModelPreview.previewWithPassword())
    }
}

@Composable
@Preview
fun LoginScreenPreview2() {
    GreenPreview {
        LoginScreen(viewModel = LoginViewModelPreview.previewWatchOnly().also {
            it.onProgress.value = false
        })
    }
}

@Composable
@Preview
fun LoginScreenPreview4() {
    GreenPreview {
        LoginScreen(viewModel = LoginViewModelPreview.previewWithDevice().also {
            it.onProgress.value = true
        })
    }
}