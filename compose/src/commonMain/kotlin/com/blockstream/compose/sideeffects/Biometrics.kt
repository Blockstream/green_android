
package com.blockstream.compose.sideeffects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.login.LoginViewModelAbstract

@Composable
expect fun rememberBiometricsState(): BiometricsState

@Stable
expect class BiometricsState {
    fun launchUserPresencePrompt(title: String, authenticated: (authenticated: Boolean) -> Unit)
    fun launchUserPresencePromptForLightningShortcut(viewModel: LoginViewModelAbstract)
    fun getBiometricsCipher(viewModel: GreenViewModel, onlyDeviceCredentials: Boolean = false)

    fun launchBiometricPrompt(
        loginCredentials: LoginCredentials,
        viewModel: LoginViewModelAbstract,
        onlyDeviceCredentials: Boolean = false
    )
}