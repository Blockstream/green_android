package com.blockstream.green.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Device
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.Banner
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.nameCleanup
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout


open class AppViewModel(val countly: Countly) : ViewModel(), HWWalletBridge, LifecycleOwner {
    internal val disposables = CompositeDisposable()

    val onEvent = MutableLiveData<ConsumableEvent<AppEvent>>()
    val onProgress = MutableLiveData(false)
    val onError = MutableLiveData<ConsumableEvent<Throwable>>()

    val banner = MutableLiveData<Banner>()
    val closedBanners = mutableListOf<Banner>()

    val onDeviceInteractionEvent = MutableLiveData<ConsumableEvent<Triple<Device, Completable?, String?>>>()

    var requestPinMatrixEmitter: CompletableDeferred<String>? = null
    var requestPinPassphraseEmitter: CompletableDeferred<String>? = null

    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.STARTED
        }
    }

    val lifecycleOwner: LifecycleOwner
        get() = this

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun interactionRequest(hw: HWWallet?, completable: Completable?, text: String?) {
        hw?.let {
            onDeviceInteractionEvent.postValue(ConsumableEvent(
                Triple(it.device, completable, text)
            ))
        }
    }

    override fun requestPinMatrix(deviceBrand: DeviceBrand?): String {
        requestPinMatrixEmitter = CompletableDeferred()

        onEvent.postValue(ConsumableEvent(AppFragment.DeviceRequestEvent.RequestPinMatrix))

        return runBlocking { requestPinMatrixEmitter!!.await() }

    }

    override fun requestPassphrase(deviceBrand: DeviceBrand?): String {
        requestPinPassphraseEmitter = CompletableDeferred()

        onEvent.postValue(ConsumableEvent(AppFragment.DeviceRequestEvent.RequestPassphrase))

        return runBlocking { requestPinPassphraseEmitter!!.await() }
    }
    
    fun deleteWallet(wallet: Wallet, sessionManager: SessionManager, walletRepository: WalletRepository, countly: Countly) {
        doUserAction({
            withContext(context = Dispatchers.IO) {
                sessionManager.destroyWalletSession(wallet)
            }
            walletRepository.deleteWallet(wallet)
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(AbstractWalletViewModel.WalletEvent.DeleteWallet))
            countly.deleteWallet()
        })
    }

    fun renameWallet(name: String, wallet: Wallet, walletRepository: WalletRepository, countly: Countly) {
        doUserAction({
            name.nameCleanup().takeIf { it.isNotBlank() }?.also {
                wallet.name = name.nameCleanup() ?: ""
                walletRepository.updateWallet(wallet)
            } ?: run { throw Exception("Name should not be blank") }
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(AbstractWalletViewModel.WalletEvent.RenameWallet))
            countly.renameWallet()
        })
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
            this.onError.value = ConsumableEvent(it)
        }
    ) {
        viewModelScope.launch {
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

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    companion object{
        // Allow to be overridden in tests
        var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    }
}