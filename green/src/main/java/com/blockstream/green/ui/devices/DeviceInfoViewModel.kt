package com.blockstream.green.ui.devices

import android.annotation.SuppressLint
import android.content.Context
import androidx.arch.core.util.Function
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.DeviceBrand
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.database.Wallet
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.devices.HardwareConnect
import com.blockstream.green.devices.HardwareConnectInteraction
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.jade.HttpRequestProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import mu.KLogging
import javax.inject.Inject

class DeviceInfoViewModel @AssistedInject constructor(
    val sessionManager: SessionManager,
    val deviceManager: DeviceManager,
    @SuppressLint("StaticFieldLeak")
    @Assisted val applicationContext: Context,
    @Assisted val device: Device
) : AppViewModel(), HardwareConnectInteraction {

    sealed class Event{
        object DeviceReady : Event()
        data class RequestPin(val deviceBrand: DeviceBrand) : Event()
        data class AskForFirmwareUpgrade(
            val deviceBrand: DeviceBrand,
            val version: String?,
            val upgradeRequired: Boolean,
            val callback: Function<Boolean?, Void?>?
        ) : Event()
        object RequestPinMatrix: Event()
        object RequestPassphrase: Event()
    }

    var requestPinEmitter: SingleEmitter<String>? = null
    var requestPinMatrixEmitter: SingleEmitter<String>? = null
    var requestPinPassphraseEmitter: SingleEmitter<String>? = null
    private val hardwareConnect = HardwareConnect()

    @Inject
    lateinit var greenWallet: GreenWallet

    var hardwareWallet: Wallet? = null

    val error = MutableLiveData<ConsumableEvent<String>>()
    val instructions = MutableLiveData<ConsumableEvent<Int>>()

    fun connectDevice(requestProvider: HttpRequestProvider, device: Device, wallet: Wallet){
        onProgress.postValue(true)
        hardwareWallet = wallet
        hardwareConnect.connectDevice(this, requestProvider, device)
    }

    override fun onCleared() {
        super.onCleared()
        hardwareConnect.onDestroy()
    }

    override fun context() = applicationContext

    override fun showInstructions(resId: Int) {
        logger.info { "Show instructions $resId" }
        instructions.postValue(ConsumableEvent(resId))
    }

    override fun getGreenSession(): GreenSession {
        return sessionManager.getHardwareSessionV3()
    }

    override fun showError(err: String) {
        logger.info { "Shown error $error" }
        error.postValue(ConsumableEvent(err))
    }

    override fun getConnectionNetwork() = greenWallet.networks.getNetworkById(hardwareWallet!!.network)

    override fun onDeviceReady() {
        logger.info { "onDeviceReady" }
        onProgress.postValue(false)

        onEvent.postValue(ConsumableEvent(Event.DeviceReady))
    }

    override fun onDeviceFailed() {
        onProgress.postValue(false)
    }

    override fun requestPin(deviceBrand: DeviceBrand): Single<String> {
        onEvent.postValue(ConsumableEvent(Event.RequestPin(deviceBrand)))

        return Single.create<String> { emitter ->
            requestPinEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    override fun askForFirmwareUpgrade(
        deviceBrand: DeviceBrand,
        version: String?,
        isUpgradeRequired: Boolean,
        callback: Function<Boolean?, Void?>?
    ) {
        onEvent.postValue(ConsumableEvent(Event.AskForFirmwareUpgrade(deviceBrand, version, isUpgradeRequired, callback)))
    }

    override fun requestPinMatrix(deviceBrand: DeviceBrand?): Single<String> {
        onEvent.postValue(ConsumableEvent(Event.RequestPinMatrix))

        return Single.create<String> { emitter ->
            requestPinMatrixEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    override fun requestPassphrase(deviceBrand: DeviceBrand?): Single<String> {
        onEvent.postValue(ConsumableEvent(Event.RequestPassphrase))

        return Single.create<String> { emitter ->
            requestPinPassphraseEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            applicationContext: Context,
            device: Device
        ): DeviceInfoViewModel
    }

    companion object : KLogging() {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            applicationContext: Context,
            device: Device
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(applicationContext, device) as T
            }
        }
    }
}