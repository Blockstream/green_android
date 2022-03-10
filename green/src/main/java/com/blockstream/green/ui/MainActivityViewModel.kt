package com.blockstream.green.ui

import android.content.Context
import androidx.lifecycle.*
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.AppKeystore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val walletRepository: WalletRepository,
    val settingsManager: SettingsManager,
    val appKeystore: AppKeystore
) : AppViewModel(), DefaultLifecycleObserver {
    private var lockTimer: Timer? = null
    val lockScreen = MutableLiveData(canLock())
    val buildVersion = MutableLiveData("")

    val wallets: LiveData<List<Wallet>> = walletRepository.getWallets()

    init {
        // Listen to foreground / background events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun canLock() = settingsManager.getApplicationSettings().enhancedPrivacy && appKeystore.canUseBiometrics(context)

    fun unlock(){
        lockScreen.value = false
    }

    override fun onResume(owner: LifecycleOwner) {
        lockTimer?.cancel()
    }

    override fun onPause(owner: LifecycleOwner) {
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
}