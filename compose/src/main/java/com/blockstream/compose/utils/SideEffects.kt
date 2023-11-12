package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.Stack
import cafe.adriel.voyager.navigator.LocalNavigator
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.LocalAppCoroutine
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.compose.screens.onboarding.AddWalletScreen
import com.blockstream.compose.screens.onboarding.EnterRecoveryPhraseScreen
import com.blockstream.compose.screens.onboarding.PinScreen
import com.blockstream.compose.screens.onboarding.SetupNewWalletScreen
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.compose.screens.recovery.RecoveryCheckScreen
import com.blockstream.compose.screens.recovery.RecoveryIntroScreen
import com.blockstream.compose.screens.recovery.RecoveryPhraseScreen
import com.blockstream.compose.screens.recovery.RecoveryWordsScreen
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.compose.sideeffects.DialogHost
import com.blockstream.compose.sideeffects.DialogState
import com.blockstream.compose.sideeffects.OpenBrowserHost
import com.blockstream.compose.sideeffects.OpenBrowserState
import com.blockstream.compose.sideeffects.OpenDialogData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

fun <Item : Screen> Stack<Item>.pushOrReplace(item: Item) {
    if(lastItemOrNull?.let { it::class == item::class } == true) {
        replace(item)
    } else {
        push(item)
    }
}

@Composable
fun HandleSideEffect(viewModel: GreenViewModel, handler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {}) {
    val snackbar = LocalSnackbar.current
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    val openBrowserState = remember { OpenBrowserState() }
    OpenBrowserHost(state = openBrowserState)

    val dialogState = remember { DialogState(context = context) }
    DialogHost(state = dialogState)

    val appCoroutine = LocalAppCoroutine.current

    LaunchedEffect(Unit) {
        viewModel.sideEffect.onEach {
            launch {
                viewModel.sideEffectReEmitted.emit(it)
            }

            when (it) {
                is SideEffects.OpenBrowser -> {
                    appCoroutine.launch {
                        openBrowserState.openBrowser(
                            context,
                            viewModel.settingsManager.appSettings.tor,
                            it.url
                        )
                    }
                }

                is SideEffects.Dialog -> {
                    appCoroutine.launch {
                        dialogState.openDialog(OpenDialogData(stringResourceIdOrNull(context, it.title), stringResourceId(context, it.message)))
                    }
                }

                is SideEffects.ErrorDialog -> {
                    appCoroutine.launch {
                        dialogState.openErrorDialog(it.error, it.errorReport)
                    }
                }

                is SideEffects.Snackbar -> {
                    appCoroutine.launch {
                        snackbar.showSnackbar(message = stringResourceId(context, it.text))
                    }
                }

                is SideEffects.NavigateBack -> {
                    if (it.error == null) {
                        navigator?.pop()
                    } else {
                        // TODO
                    }
                }

                is SideEffects.CopyToClipboard -> {
                    copyToClipboard(context = context, "Green", it.value)
                    it.message?.also {
                        appCoroutine.launch {
                            snackbar.showSnackbar(message = it)
                        }
                    }
                }

                is SideEffects.Logout -> {

                    viewModel.greenWallet.also { greenWallet ->
                        if(greenWallet.isEphemeral || greenWallet.isHardware || it.reason == LogoutReason.USER_ACTION){
                            navigator?.replaceAll(HomeScreen())
                        }else{
                            navigator?.replaceAll(
                                LoginScreen(
                                    greenWallet = greenWallet,
                                    isLightningShortcut = greenWallet.isLightning,
                                    autoLoginWallet = !greenWallet.isLightning
                                )
                            )
                        }
                    }

                    navigator?.replaceAll(HomeScreen())
                }

                is SideEffects.NavigateTo -> {
                    val destination = it.destination

                    when(destination) {
                        is NavigateDestinations.WalletOverview -> {
                            navigator?.replaceAll(
                                WalletOverviewScreen(
                                    greenWallet = destination.greenWallet,
                                )
                            )
                        }

                        is NavigateDestinations.WalletLogin -> {
                            val loginScreen = LoginScreen(
                                greenWallet = destination.greenWallet,
                                isLightningShortcut = destination.isLightningShortcut,
                                autoLoginWallet = !destination.isLightningShortcut,
                                // device = null
                            )

                            navigator?.pushOrReplace(loginScreen)

//                            (requireActivity() as MainActivity).getVisibleFragment()?.also {
//                                if(it is LoginFragment && it.viewModel.greenWalletOrNull == directions.wallet && it.args.isLightningShortcut == directions.isLightningShortcut){
//                                    return
//                                }
//                            }
//
//                            navigate(
//                                NavGraphDirections.actionGlobalLoginFragment(
//                                    wallet = directions.wallet,
//                                    isLightningShortcut = directions.isLightningShortcut,
//                                    autoLoginWallet = !directions.isLightningShortcut
//                                )
//                            )
                        }

                        is NavigateDestinations.DeviceScan -> {

                        }

                        is NavigateDestinations.AppSettings -> {
                            navigator?.push(
                                AppSettingsScreen()
                            )
                        }

                        is NavigateDestinations.About -> {
                            navigator?.push(
                                AboutScreen()
                            )
                        }

                        is NavigateDestinations.SetupNewWallet -> {
                            navigator?.push(
                                SetupNewWalletScreen()
                            )
                        }

                        is NavigateDestinations.AddWallet -> {
                            navigator?.push(
                                AddWalletScreen()
                            )
                        }

                        is NavigateDestinations.RecoveryIntro -> {
                            navigator?.push(
                                RecoveryIntroScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryWords -> {
                            navigator?.push(
                                RecoveryWordsScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryCheck -> {
                            navigator?.popUntil { it is RecoveryIntroScreen}
                            navigator?.push(
                                RecoveryCheckScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.SetPin -> {
                            navigator?.items?.firstOrNull {
                                it is RecoveryIntroScreen
                            }?.also {
                                navigator.popUntil { it is RecoveryIntroScreen}
                            }
                            navigator?.push(
                                PinScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.EnterRecoveryPhrase -> {
                            navigator?.push(
                                EnterRecoveryPhraseScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryPhrase -> {
                            navigator?.push(
                                with(destination.args) {
                                    RecoveryPhraseScreen(
                                        isLightning = isLightning,
                                        providedCredentials = credentials,
                                        greenWallet = greenWallet,
                                    )
                                }
                            )
                        }
                    }
                }

                else -> {
                    handler.invoke(this, it)
                }
            }
        }.collect()
    }
}