package com.blockstream.compose.sideeffects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.blockstream.common.database.wallet.LoginCredentials
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
    actual suspend fun launchUserPresencePrompt(
        title: String,
        authenticated: (authenticated: Boolean) -> Unit
    ) {
    }

    actual suspend fun launchUserPresencePromptForLightningShortcut(viewModel: LoginViewModelAbstract) {
    }

    actual suspend fun getBiometricsCipher(
        viewModel: GreenViewModel,
        onlyDeviceCredentials: Boolean
    ) {
    }

    actual suspend fun launchBiometricPrompt(
        loginCredentials: LoginCredentials,
        viewModel: LoginViewModelAbstract,
        onlyDeviceCredentials: Boolean
    ) {
    }

}