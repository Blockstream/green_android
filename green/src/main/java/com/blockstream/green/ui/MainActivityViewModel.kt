package com.blockstream.green.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.blockstream.common.interfaces.HttpRequestUrlValidator
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.utils.AppKeystore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.koin.android.annotation.KoinViewModel
import java.util.Timer
import kotlin.concurrent.schedule

@KoinViewModel
class MainActivityViewModel constructor(
    @SuppressLint("StaticFieldLeak") val context: Context,
    val appKeystore: AppKeystore
) : AppViewModelAndroid(), DefaultLifecycleObserver, HttpRequestUrlValidator {
    private var lockTimer: Timer? = null
    val lockScreen = MutableLiveData(canLock())
    val buildVersion = MutableLiveData("")

    var unsafeUrlWarningEmitter: CompletableDeferred<Boolean>? = null

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        sessionManager.httpRequestProvider.httpRequestUrlValidator = this
    }

    private fun canLock() = settingsManager.getApplicationSettings().enhancedPrivacy && appKeystore.canUseBiometrics()

    fun unlock(){
        lockScreen.value = false
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        lockTimer?.cancel()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if(canLock()){
            val lockAfterSeconds = settingsManager.getApplicationSettings().screenLockInSeconds
            if(lockAfterSeconds > 0){
                lockTimer = Timer().also {
                    it.schedule(lockAfterSeconds * 1000L) {
                        lockScreen.postValue(true)
                    }
                }
            }else{
                lockScreen.postValue(true)
            }
        }
    }

    override fun unsafeUrlWarning(urls: List<String>): Boolean {
        unsafeUrlWarningEmitter = CompletableDeferred()

        onEvent.postValue(ConsumableEvent(MainActivity.HttpUrlWarningEvent.UrlWarning(urls)))

        return runBlocking { unsafeUrlWarningEmitter!!.await() }
    }
}