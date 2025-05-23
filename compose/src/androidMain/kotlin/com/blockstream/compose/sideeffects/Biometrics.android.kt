package com.blockstream.compose.sideeffects

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authentication_error_s
import blockstream_green.common.generated.resources.id_authentication_failed
import blockstream_green.common.generated.resources.id_blockstream_app_uses_biometric
import blockstream_green.common.generated.resources.id_cancel
import blockstream_green.common.generated.resources.id_login_with_biometrics
import blockstream_green.common.generated.resources.id_please_activate_at_least_one
import blockstream_green.common.generated.resources.id_user_authentication
import blockstream_green.common.generated.resources.id_you_have_to_authenticate_to
import com.blockstream.common.crypto.BiometricsException
import com.blockstream.common.database.wallet.LoginCredentials
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.login.LoginViewModelAbstract
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.LocalActivity
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.extensions.showErrorSnackbar
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.managers.PlatformManager
import com.blockstream.green.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import java.security.InvalidAlgorithmParameterException
import java.security.UnrecoverableKeyException

@Composable
actual fun rememberBiometricsState(): BiometricsState {
    val activity = LocalActivity.current as FragmentActivity
    val snackbar = LocalSnackbar.current
    val dialog = LocalDialog.current
    val browserManager = LocalPlatformManager.current
    // LocalInspectionMode is true in preview
    val androidKeystore: AndroidKeystore =
        if (LocalInspectionMode.current) AndroidKeystore(activity) else koinInject()

    val scope = rememberCoroutineScope()

    return remember {
        BiometricsState(
            activity = activity,
            platformManager = browserManager,
            scope = scope,
            snackbarHostState = snackbar,
            dialogState = dialog,
            androidKeystore = androidKeystore,
        )
    }
}

@Stable
actual class BiometricsState(
    val activity: FragmentActivity,
    val platformManager: PlatformManager,
    val scope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
    val dialogState: DialogState,
    val androidKeystore: AndroidKeystore,
) {
    var activeBiometricPrompt: BiometricPrompt? = null

    private val executor = ContextCompat.getMainExecutor(activity)

    fun cancel() = activeBiometricPrompt?.cancelAuthentication()

    private suspend fun authenticateWithBiometrics(
        callback: AuthenticationCallback,
        onlyDeviceCredentials: Boolean = false
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(Res.string.id_user_authentication))
            .setDescription(getString(Res.string.id_you_have_to_authenticate_to))
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(if (onlyDeviceCredentials) BiometricManager.Authenticators.DEVICE_CREDENTIAL else BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        activeBiometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Seems there is a bug and if we execute immediately before the callback is finished
                    // the new prompt won't execute the callback
                    ContextCompat.getMainExecutor(activity).execute {
                        callback.onAuthenticationSucceeded(result)
                    }
                }
            })

        try {
            activeBiometricPrompt?.authenticate(promptInfo.build())
        } catch (e: InvalidAlgorithmParameterException) {
            // At least one biometric must be enrolled
            scope.launch {
                dialogState.openDialog(OpenDialogData(message = StringHolder.create(Res.string.id_please_activate_at_least_one)))
            }
        } catch (e: Exception) {
            scope.launch {
                dialogState.openErrorDialog(e)
            }
        }
    }

    actual suspend fun launchBiometricPrompt(
        loginCredentials: LoginCredentials,
        viewModel: LoginViewModelAbstract,
        onlyDeviceCredentials: Boolean
    ) {
        activeBiometricPrompt?.cancelAuthentication()

        val isV4Authentication = loginCredentials.keystore.isNullOrBlank()

        if (isV4Authentication && androidKeystore.isBiometricsAuthenticationRequired()) {
            authenticateWithBiometrics(object :
                AuthenticationCallback(state = this@BiometricsState) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // authenticateUserIfRequired = false prevent eternal loops
                    scope.launch {
                        launchBiometricPrompt(
                            loginCredentials = loginCredentials,
                            viewModel = viewModel,
                            onlyDeviceCredentials = true
                        )
                    }
                }
            }, onlyDeviceCredentials = onlyDeviceCredentials)
            return
        }

        loginCredentials.encrypted_data?.let { encryptedData ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(Res.string.id_login_with_biometrics))
                .setConfirmationRequired(true)

            if (isV4Authentication) {
                promptInfo
                    .setNegativeButtonText(getString(Res.string.id_cancel))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            } else {
                // V3 only needs user presence
                // Valid combinations for each SDK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                } else {
                    promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                }
            }

            activeBiometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : AuthenticationCallback(state = this@BiometricsState) {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        if (isV4Authentication) {
                            result.cryptoObject?.cipher?.let {
                                viewModel.postEvent(
                                    LoginViewModel.LocalEvents.LoginWithBiometrics(
                                        it,
                                        loginCredentials
                                    )
                                )
                            }
                        } else {
                            // Use v3 authentication system
                            try {
                                androidKeystore.getBiometricsDecryptionCipher(
                                    encryptedData,
                                    loginCredentials.keystore
                                ).also {
                                    viewModel.postEvent(
                                        LoginViewModel.LocalEvents.LoginWithBiometricsV3(
                                            it,
                                            loginCredentials
                                        )
                                    )
                                }

                            } catch (e: Exception) {
                                scope.launch {
                                    dialogState.openErrorDialog(e)
                                }
                            }
                        }
                    }
                })
            try {

                if (androidKeystore.biometricsKeyExists()) {
                    // v4 uses a default keystore and a crypto object
                    if (isV4Authentication) {
                        activeBiometricPrompt?.authenticate(
                            promptInfo.build(),
                            BiometricPrompt.CryptoObject(
                                androidKeystore.getBiometricsDecryptionCipher(
                                    encryptedData = encryptedData
                                )
                            )
                        )
                    } else {
                        // v3 required only for user to be Authenticated
                        activeBiometricPrompt?.authenticate(promptInfo.build())
                    }
                } else {
                    viewModel.postEvent(
                        LoginViewModel.LocalEvents.DeleteLoginCredentials(
                            loginCredentials
                        )
                    )
                    throw BiometricsException("Biometrics are invalidated")
                }

            } catch (e: KeyPermanentlyInvalidatedException) {
                scope.launch {
                    snackbarHostState.showErrorSnackbar(
                        platformManager = platformManager,
                        dialogState = dialogState,
                        viewModel = viewModel,
                        error = e
                    )
                }
                // Remove invalidated login credentials
                viewModel.postEvent(
                    LoginViewModel.LocalEvents.DeleteLoginCredentials(
                        loginCredentials
                    )
                )
            } catch (e: UnrecoverableKeyException) {
                scope.launch {
                    snackbarHostState.showErrorSnackbar(
                        platformManager = platformManager,
                        dialogState = dialogState,
                        viewModel = viewModel,
                        error = e
                    )
                }
                // Remove invalidated login credentials
                viewModel.postEvent(
                    LoginViewModel.LocalEvents.DeleteLoginCredentials(
                        loginCredentials
                    )
                )
            } catch (e: Exception) {
                scope.launch {
                    dialogState.openErrorDialog(e)
                }
            }
        }
    }

    actual suspend fun getBiometricsCipher(viewModel: GreenViewModel, onlyDeviceCredentials: Boolean) {

        if (androidKeystore.isBiometricsAuthenticationRequired()) {
            authenticateWithBiometrics(object :
                AuthenticationCallback(state = this@BiometricsState) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    scope.launch {
                        getBiometricsCipher(viewModel = viewModel, onlyDeviceCredentials = true)
                    }
                }
            }, onlyDeviceCredentials = onlyDeviceCredentials)
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(Res.string.id_login_with_biometrics))
            .setDescription(getString(Res.string.id_blockstream_app_uses_biometric))
            .setNegativeButtonText(getString(Res.string.id_cancel))
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : AuthenticationCallback(state = this@BiometricsState) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    result.cryptoObject?.cipher?.also {
                        viewModel.postEvent(Events.ProvideCipher(platformCipher = it))
                    } ?: kotlin.run {
                        viewModel.postEvent(Events.ProvideCipher(exception = BiometricsException("No Cipher Provided")))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    scope.launch {
                        (if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
                            Exception("id_action_canceled")
                        } else if(errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                            BiometricsException(errString.toString())
                        } else {
                            BiometricsException(
                                getString(Res.string.id_authentication_error_s,
                                    "$errorCode $errString"
                                )
                            )
                        }).also {
                            viewModel.postEvent(Events.ProvideCipher(exception = it))
                        }
                    }
                }
            })

        try {
            biometricPrompt.authenticate(
                promptInfo.build(),
                BiometricPrompt.CryptoObject(
                    androidKeystore.getBiometricsEncryptionCipher(
                        recreateKeyIfNeeded = true
                    )
                )
            )
        } catch (e: InvalidAlgorithmParameterException) {
            // At least one biometric must be enrolled
            scope.launch {
                dialogState.openDialog(OpenDialogData(message = StringHolder.create(Res.string.id_please_activate_at_least_one)))
            }
        } catch (e: Exception) {
            scope.launch {
                dialogState.openErrorDialog(e)
            }
        }
    }

    actual suspend  fun launchUserPresencePrompt(
        title: String,
        authenticated: (authenticated: Boolean?) -> Unit
    ) {
        activeBiometricPrompt?.cancelAuthentication()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setConfirmationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }

        activeBiometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : AuthenticationCallback(state = this@BiometricsState) {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL || errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                        // User hasn't enabled any device credential,
                        authenticated.invoke(null)
                    } else {
                        super.onAuthenticationError(errorCode, errString)
                        authenticated.invoke(false)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authenticated.invoke(true)
                }
            })

        try {
            // Ask for user presence
            activeBiometricPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            scope.launch {
                dialogState.openErrorDialog(e) {
                    // If an unsupported method is initiated, it's better to show the words rather than
                    // block the user from retrieving his words
                    authenticated.invoke(null)
                }
            }
        }
    }
}

open class AuthenticationCallback constructor(val state: BiometricsState) :
    BiometricPrompt.AuthenticationCallback() {

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        logger.d { "onAuthenticationError $errorCode:$errString" }
        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
            // This is errorCode OK, no need to handle it
        } else {
            // TODO INVALIDATE ALL BIOMETRIC LOGIN CREDENTIALS
            state.scope.launch {
                state.snackbarHostState.showSnackbar(
                    message = getString(
                        Res.string.id_authentication_error_s,
                        "$errorCode $errString"
                    )
                )
            }
        }
        state.activeBiometricPrompt = null
    }

    override fun onAuthenticationFailed() {
        logger.d { "onAuthenticationFailed" }
        state.scope.launch {
            state.snackbarHostState.showSnackbar(message = getString(Res.string.id_authentication_failed))
        }
        state.activeBiometricPrompt = null
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        logger.d { "onAuthenticationSucceeded" }
        super.onAuthenticationSucceeded(result)
        state.activeBiometricPrompt = null
    }

    companion object : Loggable()
}