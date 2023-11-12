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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.login.LoginViewModelAbstract
import com.blockstream.common.models.login.LoginViewModelPreview
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.AppSettingsButton
import com.blockstream.compose.components.BiometricsButton
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenSpacer
import com.blockstream.compose.components.MenuEntry
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.compose.sheets.BottomSheetNavigatorM3
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.WalletDeleteBottomSheet
import com.blockstream.compose.sheets.Bip39PassphraseBottomSheet
import com.blockstream.compose.sideeffects.BiometricsState
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.headlineMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.AppBarData
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.sheets.WalletRenameBottomSheet
import com.blockstream.compose.theme.lightning
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.views.BannerView
import com.blockstream.compose.views.PinView
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class LoginScreenCallbacks(
    val onAppSettingsClick: () -> Unit = {},
)

@Parcelize
@Serializable
data class LoginScreen(
    val greenWallet: GreenWallet,
    val isLightningShortcut: Boolean,
    val autoLoginWallet: Boolean,
//    val device: DeviceInterface? = null
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<LoginViewModel>() {
            parametersOf(greenWallet, isLightningShortcut, autoLoginWallet, null) // device
        }

        val context = LocalContext.current
        val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current

        val walletName by viewModel.walletName.collectAsStateWithLifecycle()
        val passwordCredentials by viewModel.passwordCredentials.collectAsStateWithLifecycle()
        val pinCredentials by viewModel.pinCredentials.collectAsStateWithLifecycle()

        fun bip39Passphrase() {
            bottomSheetNavigator.show(
                Bip39PassphraseBottomSheet(
                    viewModel.greenWallet, viewModel.bip39Passphrase.value
                ) { passphrase ->
                    viewModel.postEvent(
                        LoginViewModel.LocalEvents.Bip39Passphrase(passphrase, null)
                    )
                }
            )
        }

        val navigator = LocalNavigator.current


        AppBar {
            val check1 = !viewModel.isLightningShortcut && !viewModel.greenWallet.isHardware
            val check2 = check1 && !viewModel.greenWallet.isWatchOnly

            AppBarData(
                title = walletName, subtitle = if (viewModel.isLightningShortcut) context.getString(
                    R.string.id_lightning_account
                ) else null, menu = listOfNotNull(
                    MenuEntry(
                        title = context.getString(R.string.id_help),
                        iconRes = R.drawable.question,
                        showAsAction = true
                    ) {
                        viewModel.postEvent(LoginViewModel.LocalEvents.ClickHelp)
                    }.takeIf { check2 && pinCredentials.isEmpty() && passwordCredentials.isEmpty() },
                    MenuEntry(
                        title = context.getString(R.string.id_bip39_passphrase_login),
                        iconRes = R.drawable.password
                    ) {
                        bip39Passphrase()
                    }.takeIf { check2 && (pinCredentials.isNotEmpty() || passwordCredentials.isNotEmpty()) },
                    MenuEntry(
                        title = context.getString(R.string.id_show_recovery_phrase),
                        iconRes = R.drawable.key
                    ) {
                        viewModel.postEvent(LoginViewModel.LocalEvents.EmergencyRecovery(true))
                    }.takeIf { check2 && (pinCredentials.isNotEmpty() || passwordCredentials.isNotEmpty()) },
                    MenuEntry(
                        title = context.getString(R.string.id_rename_wallet),
                        iconRes = R.drawable.text_aa
                    ) {
                        bottomSheetNavigator.show(WalletRenameBottomSheet(viewModel.greenWallet))
                    }.takeIf { check1 },

                    MenuEntry(
                        title = context.getString(R.string.id_remove_wallet),
                        iconRes = R.drawable.trash
                    ) {
                        bottomSheetNavigator.show(WalletDeleteBottomSheet(viewModel.greenWallet))
                    }.takeIf { check1 },

                    )
            )
        }

        LoginScreen(viewModel = viewModel)
    }

    @IgnoredOnParcel
    override val key = uniqueScreenKey
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModelAbstract,
    callbacks: LoginScreenCallbacks = LoginScreenCallbacks()
) {
    val navigator = LocalNavigator.current
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current


    val pinCredentials by viewModel.pinCredentials.collectAsStateWithLifecycle()
    val passwordCredentials by viewModel.passwordCredentials.collectAsStateWithLifecycle()

    fun bip39Passphrase() {
        bottomSheetNavigator.show(
            Bip39PassphraseBottomSheet(
                viewModel.greenWallet, viewModel.bip39Passphrase.value
            ) { passphrase ->
                viewModel.postEvent(
                    LoginViewModel.LocalEvents.Bip39Passphrase(passphrase, null)
                )
            }
        )
    }

    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    // LocalInspectionMode is true in preview
    val androidKeystore: AndroidKeystore =
        if (LocalInspectionMode.current) AndroidKeystore(context) else koinInject()

    val dialogState = remember { DialogState(context) }
    DialogHost(state = dialogState)

    val biometricsState = remember {
        BiometricsState(
            context = context,
            coroutineScope = scope,
            snackbarHostState = snackbar,
            dialogState = dialogState,
            androidKeystore = androidKeystore
        )
    }

    HandleSideEffect(viewModel) {
        when (it) {
            is LoginViewModel.LocalSideEffects.LaunchBiometrics -> {
                biometricsState.launchBiometricPrompt(it.loginCredentials, viewModel = viewModel)
            }

            is LoginViewModel.LocalSideEffects.LaunchUserPresenceForLightning -> {
                biometricsState.launchUserPresencePromptForLightningShortcut(viewModel = viewModel)
            }

            is LoginViewModel.LocalSideEffects.AskBip39Passphrase -> {
                bip39Passphrase()
            }

            is SideEffects.WalletDelete -> {
                navigator?.popUntilRoot()
            }

            is SideEffects.Navigate -> {
                (it.data as? Credentials)?.also {
                    // TODO
//                    navigate(
//                        LoginFragmentDirections.actionGlobalRecoveryPhraseFragment(
//                            wallet = null,
//                            credentials = sideEffect.data as Credentials
//                        )
//                    )
                }
            }

            is LoginViewModel.LocalSideEffects.PinError -> {
                // binding.pinView.reset(true)
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
            GreenColumn(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(120.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    trackColor = MaterialTheme.colorScheme.secondary,
                )

                Text(text = stringResource(R.string.id_logging_in), style = labelLarge)
            }
        }

        if (viewModel.isLightningShortcut && !onProgress) {
            GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.lightning_fill),
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
                    text = stringResource(id = R.string.id_you_have_to_authenticate_using),
                    style = labelMedium
                )
            }
        }

        if (!viewModel.isLightningShortcut && pinCredentials.isEmpty() && !viewModel.greenWallet.isWatchOnly && !viewModel.greenWallet.isHardware && passwordCredentials.isEmpty() && !onProgress) {
            GreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.id_too_many_pin_attempts), style = titleSmall)

                Image(
                    painter = painterResource(id = R.drawable.shield_warning),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .aspectRatio(1f)
                )

                GreenSpacer()

                Text(
                    text = stringResource(R.string.id_youve_entered_an_invalid_pin),
                    style = labelMedium
                )

                GreenButton(text = stringResource(id = R.string.id_restore_with_recovery_phrase)) {
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
                BannerView(viewModel)
            }

            if(viewModel.greenWallet.isWatchOnly) {
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
                                    painter = painterResource(id = R.drawable.eye),
                                    contentDescription = "Watch Only",
                                    // colorFilter = ColorFilter.tint(lightning),
                                    alpha = 0.25f,
                                    modifier = Modifier
                                        .size(128.dp)
                                )
                            }

                            Text(
                                text = stringResource(R.string.id_log_in_via_watchonly_to_receive),
                                style = titleLarge
                            )

                            val showWatchOnlyUsername by viewModel.showWatchOnlyUsername.collectAsStateWithLifecycle()
                            if(showWatchOnlyUsername) {
                                val watchOnlyUsername by viewModel.watchOnlyUsername.collectAsStateWithLifecycle()
                                TextField(
                                    value = watchOnlyUsername,
                                    onValueChange = viewModel.watchOnlyUsername.onValueChange(),
                                    enabled = false,
                                    label = { Text(stringResource(R.string.id_username)) },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    singleLine = true,
                                )
                            }

                            val showWatchOnlyPassword by viewModel.showWatchOnlyPassword.collectAsStateWithLifecycle()
                            if (showWatchOnlyPassword) {
                                val watchOnlyPassword by viewModel.watchOnlyPassword.collectAsStateWithLifecycle()
                                var passwordVisibility: Boolean by remember { mutableStateOf(false) }
                                TextField(
                                    value = watchOnlyPassword,
                                    onValueChange = viewModel.watchOnlyPassword.onValueChange(),
                                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                                    label = { Text(stringResource(R.string.id_password)) },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            passwordVisibility = !passwordVisibility
                                        }) {
                                            Icon(
                                                painter = painterResource(id = if (passwordVisibility) R.drawable.eye_slash else R.drawable.eye),
                                                contentDescription = "password visibility",
                                            )
                                        }
                                    }
                                )
                            }

                            val isWatchOnlyLoginEnabled by viewModel.isWatchOnlyLoginEnabled.collectAsStateWithLifecycle()
                            GreenButton(
                                text = stringResource(id = R.string.id_log_in),
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
                                text = stringResource(id = R.string.id_enter_your_pin),
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
                                                text = stringResource(id = R.string.id_emergency_recovery_phrase),
                                                style = labelLarge,
                                                color = whiteMedium
                                            )

                                            Icon(
                                                painter = painterResource(id = R.drawable.x),
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
                                        bip39Passphrase()
                                    }) {
                                        GreenRow(padding = 0, space = 6) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.bip39_passphrase),
                                                contentDescription = null,
                                                tint = whiteMedium,
                                                modifier = Modifier.size(16.dp)
                                            )

                                            Text(
                                                text = stringResource(id = R.string.id_bip39_passphrase_login),
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
                    if (!viewModel.isLightningShortcut && pinCredentials.isNotEmpty() && !onProgress) {
                        PinView(
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.BottomCenter),
                            error = error,
                            onPin = {
                                if (it.isNotBlank()) {
                                    viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithPin(it))
                                }
                            })
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                val biometricsCredentials by viewModel.biometricsCredentials.collectAsStateWithLifecycle()
                if (!viewModel.isLightningShortcut && biometricsCredentials.isNotEmpty() && !onProgress) {
                    BiometricsButton {
                        viewModel.postEvent(LoginViewModel.LocalEvents.ClickBiometrics)
                    }
                } else {
                    GreenSpacer()
                }

                if (!onProgress) {
                    AppSettingsButton {
                        callbacks.onAppSettingsClick()
                        navigator?.push(AppSettingsScreen())
                    }
                }
            }

        }
    }
}

//@Composable
//@Preview
//fun LoginScreenPreview() {
//    GreenTheme {
//        BottomSheetNavigatorM3 {
//            LoginScreen(viewModel = LoginViewModelPreview.previewWithPin().also {
//                it.onProgress.value = false
//            })
//        }
//    }
//}

@Composable
@Preview
fun LoginScreenPreview2() {
    GreenTheme {
        BottomSheetNavigatorM3 {
            LoginScreen(viewModel = LoginViewModelPreview.previewWatchOnly().also {
                it.onProgress.value = false
            })
        }
    }
}

@Composable
@Preview
fun LoginScreenPreview3() {
    GreenTheme {
        BottomSheetNavigatorM3 {
            LoginScreen(viewModel = LoginViewModelPreview.previewWithLightningShortcut().also {
                it.onProgress.value = false
            })
        }
    }
}