package com.blockstream.green.ui

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.BuildConfig
import com.blockstream.green.data.AppEvent
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent

@KoinViewModel
open class AppViewModelAndroid constructor(greenWalletOrNull: GreenWallet? = null, accountAssetOrNull: AccountAsset? = null) : GreenViewModel(greenWalletOrNull = greenWalletOrNull, accountAssetOrNull = accountAssetOrNull), HardwareWalletInteraction, KoinComponent {
    val onEvent = MutableLiveData<ConsumableEvent<AppEvent>>()
    val onError = MutableLiveData<ConsumableEvent<Throwable>>()

    init {
        bootstrap()
    }

    fun <T> doUserAction(
        action: suspend () -> T,
        timeout: Long = 0,
        preAction: (() -> Unit)? = {
            onProgress.value = true
        },
        postAction: ((Exception?) -> Unit)? = {
            onProgress.value = false
        },
        onSuccess: (T) -> Unit,
        onError: ((Throwable) -> Unit) = {
            if (BuildConfig.DEBUG) {
                it.printStackTrace()
            }
            this.onError.value = ConsumableEvent(it)
        }
    ) {
        viewModelScope.coroutineScope.launch {
            preAction?.invoke()

            try {
                withContext(context = ioDispatcher) {
                    if(timeout <= 0L) {
                        action.invoke()
                    }else{
                        withTimeout(timeout) {
                            action.invoke()
                        }
                    }
                }.also {
                    postAction?.invoke(null)
                    onSuccess.invoke(it)
                }
            } catch (e: Exception) {
                countly.recordException(e)

                postAction?.invoke(e)
                onError.invoke(e)
            }
        }
    }

    companion object{
        // Allow to be overridden in tests
        var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    }
}