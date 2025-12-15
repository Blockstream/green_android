package com.blockstream.compose.models.onboarding.watchonly

import androidx.lifecycle.viewModelScope
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.data.WatchOnlyCredentials
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

abstract class WatchOnlyMultisigViewModelAbstract(val setupArgs: SetupArgs) : GreenViewModel() {
    override fun screenName(): String = "OnBoardWatchOnlyMultisig"

    override fun segmentation(): HashMap<String, Any>? =
        setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }

    abstract val isLoginEnabled: StateFlow<Boolean>
    abstract val username: MutableStateFlow<String>
    abstract val password: MutableStateFlow<String>
    abstract val isRememberMe: MutableStateFlow<Boolean>
}

class WatchOnlyMultisigViewModel(setupArgs: SetupArgs) :
    WatchOnlyMultisigViewModelAbstract(setupArgs = setupArgs) {

    override val username: MutableStateFlow<String> = MutableStateFlow("")
    override val password: MutableStateFlow<String> = MutableStateFlow("")
    override val isRememberMe: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override val isLoginEnabled: StateFlow<Boolean> = combine(
        username,
        password,
        onProgress
    ) { username, password, onProgress ->
        !onProgress && username.isNotBlank() && password.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is Events.Continue -> {
                createMultisigWatchOnlyWallet()
            }
        }
    }

    private fun createMultisigWatchOnlyWallet() {
        val watchOnlyCredentials = WatchOnlyCredentials(
            username = username.value,
            password = password.value
        )

        // Use the network from setupArgs (should be a Green network for multisig)
        val network = setupArgs.network ?: session.networks.bitcoinGreen

        createNewWatchOnlyWallet(
            network = network,
            persistLoginCredentials = isRememberMe.value,
            watchOnlyCredentials = watchOnlyCredentials,
        )
    }
}