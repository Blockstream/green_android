package com.blockstream.green.ui

import com.blockstream.common.interfaces.JadeHttpRequestUrlValidator
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

class MainActivityViewModel constructor(
    private val lifecycleManager: LifecycleManager
) : GreenViewModel(), JadeHttpRequestUrlValidator {
    val lockScreen = lifecycleManager.isLocked
    val buildVersion = MutableStateFlow("")

    var unsafeUrlWarningEmitter: CompletableDeferred<Boolean>? = null
    var torWarningEmitter: CompletableDeferred<Boolean>? = null

    init {
        sessionManager.httpRequestHandler.jadeHttpRequestUrlValidator = this
    }

    fun unlock(){
        lifecycleManager.unlock()
    }

    override fun unsafeUrlWarning(urls: List<String>): Boolean {
        unsafeUrlWarningEmitter = CompletableDeferred()

        postSideEffect(SideEffects.UrlWarning(urls))

        return runBlocking { unsafeUrlWarningEmitter!!.await() }
    }

    override fun torWarning(): Boolean {
        torWarningEmitter = CompletableDeferred()

        postSideEffect(SideEffects.TorWarning)

        return runBlocking { torWarningEmitter!!.await() }
    }
}