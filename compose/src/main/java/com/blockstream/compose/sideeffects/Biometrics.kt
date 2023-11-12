
package com.blockstream.compose.sideeffects

import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.login.LoginViewModelAbstract
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.AndroidKeystore
import com.blockstream.compose.R
import com.blockstream.compose.extensions.showErrorSnackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.InvalidAlgorithmParameterException
import java.security.UnrecoverableKeyException

@Stable
class BiometricsState constructor(
    val context: Context,
    val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
    val dialogState: DialogState,
     val androidKeystore: AndroidKeystore
) {
    private var biometricPrompt : BiometricPrompt? = null

    private val executor = ContextCompat.getMainExecutor(context)

    fun authenticateWithBiometrics(callback : AuthenticationCallback, onlyDeviceCredentials: Boolean = false){
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.id_user_authentication))
            .setDescription(context.getString(R.string.id_you_have_to_authenticate_to_unlock_your_device))
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(if (onlyDeviceCredentials) BiometricManager.Authenticators.DEVICE_CREDENTIAL else BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Seems there is a bug and if we execute immediately before the callback is finished
                    // the new prompt won't execute the callback
                    ContextCompat.getMainExecutor(context).execute {
                        callback.onAuthenticationSucceeded(result)
                    }
                }
            })

        try {
            biometricPrompt.authenticate(promptInfo.build())
        }catch (e: InvalidAlgorithmParameterException){
            // At least one biometric must be enrolled
            coroutineScope.launch {
                dialogState.openDialog(OpenDialogData(message = context.getString(R.string.id_please_activate_at_least_one)))
            }
        } catch (e: Exception) {
            coroutineScope.launch {
                dialogState.openErrorDialog(e)
            }
        }
    }

    fun launchBiometricPrompt(loginCredentials: LoginCredentials, viewModel: LoginViewModelAbstract, onlyDeviceCredentials: Boolean = false) {
        biometricPrompt?.cancelAuthentication()

        val isV4Authentication = loginCredentials.keystore.isNullOrBlank()

        if(isV4Authentication && androidKeystore.isBiometricsAuthenticationRequired()){
            authenticateWithBiometrics(object : AuthenticationCallback(context = context, coroutineScope = coroutineScope, snackbarHostState = snackbarHostState) {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // authenticateUserIfRequired = false prevent eternal loops
                    launchBiometricPrompt(loginCredentials = loginCredentials, viewModel = viewModel, onlyDeviceCredentials = true)
                }
            }, onlyDeviceCredentials = onlyDeviceCredentials)
            return
        }

        loginCredentials.encrypted_data?.let { encryptedData ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.id_login_with_biometrics))
                .setConfirmationRequired(true)

            if(isV4Authentication){
                promptInfo
                    .setNegativeButtonText(context.getString(R.string.id_cancel))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            }else{
                // V3 only needs user presence
                // Valid combinations for each SDK
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                } else {
                    promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                }
            }

            biometricPrompt = BiometricPrompt(
                context as FragmentActivity,
                ContextCompat.getMainExecutor(context),
                object : AuthenticationCallback(context = context, coroutineScope = coroutineScope, snackbarHostState = snackbarHostState) {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (isV4Authentication) {
                            result.cryptoObject?.cipher?.let {
                                viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithBiometrics(it, loginCredentials))
                            }
                        } else {
                            // Use v3 authentication system
                            try {
                                androidKeystore.getBiometricsDecryptionCipher(
                                    encryptedData,
                                    loginCredentials.keystore
                                ).also {
                                    viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithBiometricsV3(it, loginCredentials))
                                }

                            } catch (e: Exception) {
                                coroutineScope.launch {
                                    dialogState.openErrorDialog(e)
                                }
                            }
                        }
                    }
                })
            try {

                // v4 uses a default keystore and a crypto object
                if(isV4Authentication){
                    biometricPrompt?.authenticate(
                        promptInfo.build(),
                        BiometricPrompt.CryptoObject(
                            androidKeystore.getBiometricsDecryptionCipher(
                                encryptedData = encryptedData
                            )
                        )
                    )
                }else{
                    // v3 required only for user to be Authenticated
                    biometricPrompt?.authenticate(promptInfo.build())
                }

            } catch (e: KeyPermanentlyInvalidatedException) {
                coroutineScope.launch {
                    snackbarHostState.showErrorSnackbar(e)
                }
                // Remove invalidated login credentials
                viewModel.postEvent(LoginViewModel.LocalEvents.DeleteLoginCredentials(loginCredentials))
            } catch (e: UnrecoverableKeyException) {
                coroutineScope.launch {
                    snackbarHostState.showErrorSnackbar(e)
                }
                // Remove invalidated login credentials
                viewModel.postEvent(LoginViewModel.LocalEvents.DeleteLoginCredentials(loginCredentials))
            } catch (e: Exception) {
                coroutineScope.launch {
                    dialogState.openErrorDialog(e)
                }
            }
        }
    }

    fun launchUserPresencePromptForLightningShortcut(viewModel: LoginViewModelAbstract) {
        biometricPrompt?.cancelAuthentication()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.id_authenticate))
            .setConfirmationRequired(true)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }

        biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            ContextCompat.getMainExecutor(context),
            object : AuthenticationCallback(context = context, coroutineScope = coroutineScope, snackbarHostState = snackbarHostState) {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    if(errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL){
                        // User hasn't enabled any device credential,
                        viewModel.postEvent(LoginViewModel.LocalEvents.LoginLightningShortcut(true))
                    }else{
                        super.onAuthenticationError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.postEvent(LoginViewModel.LocalEvents.LoginLightningShortcut(true))
                }
            })

        try {
            // Ask for user presence
            biometricPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            coroutineScope.launch {
                dialogState.openErrorDialog(e) {
                    // If an unsupported method is initiated, it's better to show the words rather than
                    // block the user from retrieving his words
                    viewModel.postEvent(LoginViewModel.LocalEvents.LoginLightningShortcut(true))
                }
            }
        }
    }

    fun launchUserPresencePrompt(
        authenticated: (authenticated: Boolean) -> Unit
    ) {
        biometricPrompt?.cancelAuthentication()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.id_authenticate_to_view_the))
            .setConfirmationRequired(true)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            promptInfo.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        }

        biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            ContextCompat.getMainExecutor(context),
            object : AuthenticationCallback(context = context, coroutineScope = coroutineScope, snackbarHostState = snackbarHostState) {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    if(errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL){
                        // User hasn't enabled any device credential,
                        authenticated.invoke(false)
                    }else{
                        super.onAuthenticationError(errorCode, errString)
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticated.invoke(true)
                }
            })

        try {
            // Ask for user presence
            biometricPrompt?.authenticate(promptInfo.build())
        } catch (e: Exception) {
            coroutineScope.launch {
                dialogState.openErrorDialog(e) {
                    // If an unsupported method is initiated, it's better to show the words rather than
                    // block the user from retrieving his words
                    authenticated.invoke(false)
                }
            }
        }
    }
}

open class AuthenticationCallback constructor(val context: Context, val coroutineScope: CoroutineScope, val snackbarHostState: SnackbarHostState) : BiometricPrompt.AuthenticationCallback() {

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
            // This is errorCode OK, no need to handle it
        } else {
            // TODO INVALIDATE ALL BIOMETRIC LOGIN CREDENTIALS
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = context.getString(R.string.id_authentication_error_s, "$errorCode $errString"))
            }
        }
    }

    override fun onAuthenticationFailed() {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message = context.getString(R.string.id_authentication_failed))
        }
    }
}