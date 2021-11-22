package com.blockstream.green.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.blockstream.green.R
import java.security.InvalidAlgorithmParameterException

fun Fragment.authenticateWithBiometrics(callback : AuthenticationCallback){
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.id_biometrics_authentication))
        .setDescription(getString(R.string.id_you_have_to_authenticate_using))
        .setNegativeButtonText(getString(R.string.id_cancel))
        .setConfirmationRequired(false)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

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

open class AuthenticationCallback(val fragment: Fragment) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
            // This is errorCode OK, no need to handle it
        } else {
            // TODO INVALIDATE ALL BIOMETRIC LOGIN CREDENTIALS
            fragment.toast(fragment.getString(R.string.id_authentication_error_s, "$errorCode $errString"))
        }
    }

    override fun onAuthenticationFailed() {
        fragment.toast(fragment.getString(R.string.id_authentication_failed))
    }
}