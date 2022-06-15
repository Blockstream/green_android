package com.blockstream.green.ui

import androidx.lifecycle.*
import com.blockstream.DeviceBrand
import com.blockstream.gdk.data.Device
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.logException
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


open class AppViewModel : ViewModel(), HWWalletBridge, LifecycleOwner {

    internal val disposables = CompositeDisposable()

    val onEvent = MutableLiveData<ConsumableEvent<AppEvent>>()
    val onProgress = MutableLiveData(false)
    val onError = MutableLiveData<ConsumableEvent<Throwable>>()

    val onDeviceInteractionEvent = MutableLiveData<ConsumableEvent<Triple<Device, Completable?, String?>>>()

    var requestPinMatrixEmitter: SingleEmitter<String>? = null
    var requestPinPassphraseEmitter: SingleEmitter<String>? = null

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

    override fun requestPinMatrix(deviceBrand: DeviceBrand?): Single<String> {
        onEvent.postValue(ConsumableEvent(AppFragment.DeviceRequestEvent.RequestPinMatrix))

        return Single.create<String> { emitter ->
            requestPinMatrixEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    override fun requestPassphrase(deviceBrand: DeviceBrand?): Single<String> {
        onEvent.postValue(ConsumableEvent(AppFragment.DeviceRequestEvent.RequestPassphrase))

        return Single.create<String> { emitter ->
            requestPinPassphraseEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
    }
    
    fun deleteWallet(wallet: Wallet, sessionManager: SessionManager, walletRepository: WalletRepository, countly: Countly) {

        viewModelScope.launch(context = logException(countly, onError = onError)) {
            withContext(context = Dispatchers.IO) {
                sessionManager.destroyWalletSession(wallet)
            }
            walletRepository.deleteWalletSuspend(wallet)

            onEvent.postValue(ConsumableEvent(AbstractWalletViewModel.WalletEvent.DeleteWallet))
            countly.deleteWallet()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}