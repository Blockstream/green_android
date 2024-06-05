package com.blockstream.compose.sideeffects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.login.LoginViewModelAbstract

@Composable
actual fun rememberBiometricsState(): BiometricsState {
    return remember {
        BiometricsState()
    }
}

@Stable
actual class BiometricsState {
    actual fun launchUserPresencePrompt(
        title: String,
        authenticated: (authenticated: Boolean) -> Unit
    ) {
        // authenticated.invoke(true)
    }

    actual fun launchUserPresencePromptForLightningShortcut(viewModel: LoginViewModelAbstract) {
    }

    actual fun getBiometricsCipher(
        viewModel: GreenViewModel,
        onlyDeviceCredentials: Boolean
    ) {
    }

    actual fun launchBiometricPrompt(
        loginCredentials: LoginCredentials,
        viewModel: LoginViewModelAbstract,
        onlyDeviceCredentials: Boolean
    ) {
    }

}