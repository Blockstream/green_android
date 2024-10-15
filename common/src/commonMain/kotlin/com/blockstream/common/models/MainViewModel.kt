package com.blockstream.common.models

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.interfaces.JadeHttpRequestUrlValidator
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import kotlinx.coroutines.CompletableDeferred
import org.koin.core.component.inject

class MainViewModel : GreenViewModel(), JadeHttpRequestUrlValidator {
    private val lifecycleManager: LifecycleManager by inject()

    val lockScreen = lifecycleManager.isLocked

    private var unsafeUrls: List<String>? = null
    private var unsafeUrlWarningEmitter: CompletableDeferred<Boolean>? = null
    private var torWarningEmitter: CompletableDeferred<Boolean>? = null

    class LocalEvents {
        data class UrlWarningResponse(val allow: Boolean, val remember: Boolean) : Event
        data class TorWarningResponse(val enable: Boolean) : Event
    }

    init {
        sessionManager.httpRequestHandler.jadeHttpRequestUrlValidator = this

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.UrlWarningResponse) {
            unsafeUrls?.also {
                if(event.remember && event.allow){
                    settingsManager.setAllowCustomPinServer(it)
                }
            }

            unsafeUrlWarningEmitter?.complete(event.allow)
            unsafeUrlWarningEmitter = null
        } else if(event is LocalEvents.TorWarningResponse) {
            settingsManager.saveApplicationSettings(
                settingsManager.getApplicationSettings().copy(tor = true)
            )
            torWarningEmitter?.complete(event.enable)
            torWarningEmitter = null
        }
    }

    fun unlock() {
        lifecycleManager.unlock()
    }

    override suspend fun unsafeUrlWarning(urls: List<String>): Boolean =
        CompletableDeferred<Boolean>().also {
            unsafeUrlWarningEmitter = it
            unsafeUrls = urls
            postSideEffect(SideEffects.UrlWarning(urls))
        }.await()

    override suspend fun torWarning(): Boolean = CompletableDeferred<Boolean>().also {
        torWarningEmitter = it
        postSideEffect(SideEffects.TorWarning)
    }.await()

    fun navigate(wallet: GreenWallet, deviceId: String?, isLightningShortcut: Boolean) {
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.Login(
                    greenWallet = wallet,
                    deviceId = deviceId,
                    isLightningShortcut = isLightningShortcut
                )
            )
        )
        postSideEffect(SideEffects.CloseDrawer)
    }
}