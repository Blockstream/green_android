package com.blockstream.green.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.data.Countly
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.jade.HttpRequestUrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.schedule

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    @SuppressLint("StaticFieldLeak")
    @ApplicationContext val context: Context,
    val walletRepository: WalletRepository,
    val settingsManager: SettingsManager,
    val appKeystore: AppKeystore,
    val sessionManager: SessionManager,
    countly: Countly
) : AppViewModel(countly), DefaultLifecycleObserver, HttpRequestUrlValidator {
    private var lockTimer: Timer? = null
    val lockScreen = MutableLiveData(canLock())
    val buildVersion = MutableLiveData("")

    var unsafeUrlWarningEmitter: CompletableDeferred<Boolean>? = null

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        sessionManager.httpRequestProvider.httpRequestUrlValidator = this
    }

    private fun canLock() = settingsManager.getApplicationSettings().enhancedPrivacy && appKeystore.canUseBiometrics(context)

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