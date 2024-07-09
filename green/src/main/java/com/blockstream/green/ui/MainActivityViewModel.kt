package com.blockstream.green.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.interfaces.JadeHttpRequestUrlValidator
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MainActivityViewModel constructor(
    @SuppressLint("StaticFieldLeak") val context: Context,
    val lifecycleManager: LifecycleManager
) : GreenViewModel(), JadeHttpRequestUrlValidator {
    val lockScreen = lifecycleManager.isLocked
    val buildVersion = MutableLiveData("")

    var unsafeUrlWarningEmitter: CompletableDeferred<Boolean>? = null
    var torWarningEmitter: CompletableDeferred<Boolean>? = null

    init {
        sessionManager.httpRequestProvider.jadeHttpRequestUrlValidator = this
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