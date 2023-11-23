package com.blockstream.green.extensions

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.blockstream.green.R
import java.security.InvalidAlgorithmParameterException

fun Fragment.authenticateWithBiometrics(callback : AuthenticationCallback, onlyDeviceCredentials: Boolean = false){
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.id_user_authentication))
        .setDescription(getString(R.string.id_you_have_to_authenticate_to_unlock_your_device))
        .setConfirmationRequired(true)
        .setAllowedAuthenticators(if (onlyDeviceCredentials) BiometricManager.Authenticators.DEVICE_CREDENTIAL else BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

    val biometricPrompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(requireContext()),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Seems there is a bug and if we execute immediately before the callback is finished
                // the new prompt won't execute the callback
                ContextCompat.getMainExecutor(requireContext()).execute {
                    callback.onAuthenticationSucceeded(result)
                }
            }
        })

    try {
        biometricPrompt.authenticate(promptInfo.build())
    }catch (e: InvalidAlgorithmParameterException){
        // At least one biometric must be enrolled
        errorDialog(getString(R.string.id_please_activate_at_least_one))
    } catch (e: Exception) {
        errorDialog(e)
    }
}

open class AuthenticationCallback constructor(val context: Context) : BiometricPrompt.AuthenticationCallback() {

    constructor(fragment: Fragment) : this(fragment.requireContext())

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
            // This is errorCode OK, no need to handle it
        } else {
            // TODO INVALIDATE ALL BIOMETRIC LOGIN CREDENTIALS
            Toast.makeText(context, context.getString(R.string.id_authentication_error_s, "$errorCode $errString"), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthenticationFailed() {
        Toast.makeText(context, context.getString(R.string.id_authentication_failed), Toast.LENGTH_SHORT).show()
    }
}