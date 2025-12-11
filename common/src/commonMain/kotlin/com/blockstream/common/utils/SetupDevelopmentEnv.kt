package com.blockstream.common.utils

import com.blockstream.common.data.DataState
import com.blockstream.common.events.Events
import com.blockstream.common.models.login.LoginViewModel
import com.blockstream.common.models.login.LoginViewModelAbstract
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.data.config.AppInfo
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper class for development environment features.
 * 
 * This class provides utilities to make development and testing easier,
 * such as automatic login with a pre-configured PIN.
 */
class SetupDevelopmentEnv : KoinComponent {
    private val appInfo: AppInfo by inject()
    
    /**
     * Attempts to auto-login using the development PIN if:
     * 1. The app is running in development/debug mode
     * 2. A development PIN is configured in local.properties (DEV_PIN_CODE)
     * 3. PIN credentials are available for the wallet
     * 
     * This should be called from LoginViewModel's init block after credentials are loaded.
     */
    fun tryAutoLogin(viewModel: LoginViewModelAbstract) {
        // Only proceed if we're in development/debug mode with a configured PIN
        if (!appInfo.isDevelopmentOrDebug || appInfo.developmentPin.isNullOrBlank()) {
            return
        }
        
        // Check if PIN credentials are available
        viewModel.viewModelScope.launch {
            // Wait for credentials to be loaded
            val pinCredentialsState = viewModel.pinCredentials.first { it !is DataState.Loading }
            
            // If PIN credentials are available, attempt auto-login
            if (pinCredentialsState is DataState.Success) {
                viewModel.postEvent(LoginViewModel.LocalEvents.LoginWithPin(appInfo.developmentPin!!))
                viewModel.postEvent(Events.EventSideEffect(SideEffects.Snackbar(StringHolder(string = "Login with development PIN"))))
            }
        }
    }
}