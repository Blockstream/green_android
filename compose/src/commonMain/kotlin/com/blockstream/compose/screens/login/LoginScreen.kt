package com.blockstream.compose.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.ColorFilter
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
import blockstream_green.common.generated.resources.id_bip39_passphrase_login
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
import blockstream_green.common.generated.resources.id_you_have_to_authenticate_using
import blockstream_green.common.generated.resources.id_youve_entered_an_invalid_pin
import blockstream_green.common.generated.resources.lightning_fill
import blockstream_green.common.generated.resources.shield_warning
import blockstream_green.common.generated.resources.tor
import blockstream_green.common.generated.resources.x
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.login.LoginViewModelAbstract
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.LocalRootNavigator
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.Banner
import com.blockstream.compose.components.BiometricsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.RichWatchOnlyButton
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.Bip39PassphraseBottomSheet
import com.blockstream.compose.sideeffects.rememberBiometricsState
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.headlineMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.lightning
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AlphaPulse
import com.blockstream.compose.utils.AnimatedNullableVisibility
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.TextInputPassword
import com.blockstream.compose.views.PinView
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf


@Parcelize
@Serializable
data class LoginScreen(
    val greenWallet: GreenWallet,
    val isLightningShortcut: Boolean,
    val autoLoginWallet: Boolean,
    val deviceId: String? = null
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<LoginViewModel>() {
            parametersOf(greenWallet, isLightningShortcut, autoLoginWallet, deviceId)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()
        AppBar(navData)

        LoginScreen(viewModel = viewModel)
    }

    @IgnoredOnParcel
    override val key = uniqueScreenKey
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModelAbstract,
) {
    val navigator = LocalRootNavigator.current

    val pinCredentials by viewModel.pinCredentials.collectAsStateWithLifecycle()
    val passwordCredentials by viewModel.passwordCredentials.collectAsStateWithLifecycle()

    Bip39PassphraseBottomSheet.getResult {
        viewModel.postEvent(
            LoginViewModel.LocalEvents.Bip39Passphrase(it, null)
        )
    }

    val biometricsState = rememberBiometricsState()

    HandleSideEffect(viewModel) {
        when (it) {
            is LoginViewModel.LocalSideEffects.LaunchBiometrics -> {
                biometricsState.launchBiometricPrompt(it.loginCredentials, viewModel = viewModel)
            }

            is LoginViewModel.LocalSideEffects.LaunchUserPresenceForLightning -> {
                biometricsState.launchUserPresencePromptForLightningShortcut(viewModel = viewModel)
            }

            is SideEffects.WalletDelete -> {
                navigator?.popUntilRoot()
            }
        }
    }

    val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

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

                    viewModel.device?.also {
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
                        style = bodySmall,
                        color = whiteMedium
                    )
                }
            }
        }

        if (viewModel.isLightningShortcut && !onProgress) {
            GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(Res.drawable.lightning_fill),
                    contentDescription = "Lightning",
                    colorFilter = ColorFilter.tint(lightning),
                    modifier = Modifier
                        .size(128.dp)
                        .clickable {
                            viewModel.postEvent(
                                LoginViewModel.LocalEvents.LoginLightningShortcut(
                                    false
                                )
                            )
                        }
                )

                Text(
                    text = stringResource(Res.string.id_you_have_to_authenticate_using),
                    style = labelMedium
                )
            }
        }

        if (!viewModel.isLightningShortcut && pinCredentials.isEmpty() && !viewModel.greenWallet.isWatchOnly && !viewModel.greenWallet.isHardware && passwordCredentials.isEmpty() && !onProgress) {
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
                .padding(start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            if (!onProgress) {
                Banner(viewModel)
            }

            if (viewModel.greenWallet.isWatchOnly) {
                if (!onProgress) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {

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
                                Image(
                                    painter = painterResource(Res.drawable.eye),
                                    contentDescription = "Watch Only",
                                    // colorFilter = ColorFilter.tint(lightning),
                                    alpha = 0.25f,
                                    modifier = Modifier
                                        .size(128.dp)
                                )
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

                    if (!viewModel.isLightningShortcut && (pinCredentials.isNotEmpty() || passwordCredentials.isNotEmpty()) && !onProgress) {
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
                                                viewModel.bip39Passphrase.value
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
                    if(!viewModel.isLightningShortcut && !onProgress){
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

                                GreenButton(text = stringResource(Res.string.id_log_in), modifier = Modifier.fillMaxWidth()) {
                                    viewModel.postEvent(
                                        LoginViewModel.LocalEvents.LoginWithPin(password)
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

                val biometricsCredentials by viewModel.biometricsCredentials.collectAsStateWithLifecycle()
                if (!viewModel.isLightningShortcut && biometricsCredentials.isNotEmpty() && !onProgress) {
                    BiometricsButton(modifier = Modifier.align(Alignment.CenterStart)) {
                        viewModel.postEvent(LoginViewModel.LocalEvents.ClickBiometrics)
                    }
                }

                val richWatchOnlyCredentials by viewModel.richWatchOnlyCredentials.collectAsStateWithLifecycle()

                if(!onProgress && richWatchOnlyCredentials.isNotEmpty()){
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
