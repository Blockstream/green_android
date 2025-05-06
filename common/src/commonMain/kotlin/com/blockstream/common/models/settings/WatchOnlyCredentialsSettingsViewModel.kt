package com.blockstream.common.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.common.data.GreenWallet
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString


abstract class WatchOnlyCredentialsSettingsViewModelAbstract(
    greenWallet: GreenWallet,
    val network: Network
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "WatchOnlyCredentials"

    @NativeCoroutinesState
    abstract val username: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val password: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val hasWatchOnlyCredentials: StateFlow<Boolean>
}

class WatchOnlyCredentialsSettingsViewModel(greenWallet: GreenWallet, network: Network) :
    WatchOnlyCredentialsSettingsViewModelAbstract(greenWallet = greenWallet, network = network) {

    override val username: MutableStateFlow<String> = MutableStateFlow("")
    override val password: MutableStateFlow<String> = MutableStateFlow("")

    private val _hasWatchOnlyCredentials: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasWatchOnlyCredentials = _hasWatchOnlyCredentials.asStateFlow()

    class LocalEvents {
        object DeleteCredentials : Event
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_watchonly))
        }

        session.watchOnlyUsername(network).onEach {
            username.value = it ?: ""
            _hasWatchOnlyCredentials.value = it.isNotBlank()
        }.launchIn(this)

        combine(username, password) { username, password ->
            _isValid.value = username.length >= 8 && password.length >= 8
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            updateWatchOnly()
        } else if (event is LocalEvents.DeleteCredentials) {
            deleteWatchOnly()
        }
    }

    private fun updateWatchOnly() {
        doAsync({
            session.setWatchOnly(
                network = network,
                username = username.value.trim(),
                password = password.value.trim()
            )
        }, onSuccess = {
            postSideEffect(SideEffects.Dismiss)
        })
    }

    private fun deleteWatchOnly() {
        doAsync({
            session.deleteWatchOnly(network = network)
        }, onSuccess = {
            postSideEffect(SideEffects.Dismiss)
        })
    }
}

class WatchOnlyCredentialsSettingsViewModelPreview(greenWallet: GreenWallet) :
    WatchOnlyCredentialsSettingsViewModelAbstract(
        greenWallet = greenWallet,
        network = previewNetwork()
    ) {

    override val username: MutableStateFlow<String> = MutableStateFlow("username")
    override val password: MutableStateFlow<String> = MutableStateFlow("")
    override val hasWatchOnlyCredentials: MutableStateFlow<Boolean> = MutableStateFlow(true)

    companion object {
        fun preview() =
            WatchOnlyCredentialsSettingsViewModelPreview(previewWallet(isHardware = false))
    }
}