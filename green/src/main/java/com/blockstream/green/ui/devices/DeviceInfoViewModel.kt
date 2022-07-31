package com.blockstream.green.ui.devices

import android.annotation.SuppressLint
import android.content.Context
import androidx.arch.core.util.Function
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.blockstream.DeviceBrand
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.devices.HardwareConnect
import com.blockstream.green.devices.HardwareConnectInteraction
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.isDevelopmentFlavor
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.kotlin.subscribeBy
import mu.KLogging
import javax.inject.Inject

class DeviceInfoViewModel @AssistedInject constructor(
    val sessionManager: SessionManager,
    val deviceManager: DeviceManager,
    val qaTester: QATester,
    @SuppressLint("StaticFieldLeak")
    @Assisted val applicationContext: Context,
    @Assisted val device: Device
) : AppViewModel(), HardwareConnectInteraction {

    sealed class DeviceInfoEvent : AppEvent {
        data class RequestPin(val deviceBrand: DeviceBrand) : DeviceInfoEvent()
        data class AskForFirmwareUpgrade(
            val request: FirmwareUpgradeRequest,
            val callback: Function<Int?, Void>
        ) : DeviceInfoEvent()
    }

    var requestPinEmitter: SingleEmitter<String>? = null

    private val hardwareConnect = HardwareConnect(qaTester, isDevelopmentFlavor)

    @Inject
    lateinit var greenWallet: GreenWallet

    val error = MutableLiveData<ConsumableEvent<String>>()
    val instructions = MutableLiveData<ConsumableEvent<Int>>()

    var session = sessionManager.getOnBoardingSession()

    val deviceState = device.deviceState.asLiveData()

    fun connectDeviceToNetwork(network: String){
        // Device is unlocked
        if (device.hwWallet != null) {
            sessionManager.getDeviceSessionForNetwork(device, network)?.also {
                switchSessions(it)
            } ?: run {
                connectOnNetwork(network)
            }

        } else {
            unlockDeviceAndConnect(network)
        }
    }

    private fun switchSessions(session: GreenSession){
        onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(session.ephemeralWallet)))
    }

    private fun connectOnNetwork(network: String){
        session = sessionManager.getOnBoardingSession()

        session.observable { session ->
            session.disconnect()
            session.ephemeralWallet = Wallet.createEphemeralWallet(ephemeralId = 0, greenWallet.networks.getNetworkById(network), isHardware = true)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onSuccess = {
                onDeviceReady()
            },
            onError = {
                it.printStackTrace()
            }
        )
    }

    private fun unlockDeviceAndConnect(network: String){
        session = sessionManager.getOnBoardingSession()

        session.observable { session ->
            // Disconnect any previous hww connection
            session.disconnect()
            session.ephemeralWallet = Wallet.createEphemeralWallet(ephemeralId = 0, greenWallet.networks.getNetworkById(network), isHardware = true)
            hardwareConnect.connectDevice(this, session, device)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.subscribeBy(
            onError = {
                it.printStackTrace()
            }
        )
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
        return session
    }

    override fun getConnectionNetwork() = getGreenSession().networkFromWallet(getGreenSession().ephemeralWallet!!)

    override fun showError(err: String) {
        logger.info { "Shown error $err" }
        error.postValue(ConsumableEvent(err))
    }

    override fun onDeviceReady() {
        onProgress.postValue(false)

        session.ephemeralWallet?.let {
            sessionManager.upgradeOnBoardingSessionToWallet(it)
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        }
    }

    override fun onDeviceFailed() {
        onProgress.postValue(false)
    }

    override fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest,
        callback: Function<Int?, Void>
    ) {
        onEvent.postValue(ConsumableEvent(DeviceInfoEvent.AskForFirmwareUpgrade(firmwareUpgradeRequest, callback)))
    }

    override fun firmwareUpdated(requireReconnection: Boolean, requireBleRebonding: Boolean) {
        if(requireBleRebonding){
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(REQUIRE_REBONDING)))
        }else if(requireReconnection) {
            // on firmware update, navigate to device list
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateBack()))
        }
    }

    override fun requestPin(deviceBrand: DeviceBrand): Single<String> {
        onEvent.postValue(ConsumableEvent(DeviceInfoEvent.RequestPin(deviceBrand)))

        return Single.create<String> { emitter ->
            requestPinEmitter = emitter
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    override fun getAntiExfilCorruptionForMessageSign() = qaTester.getAntiExfilCorruptionForMessageSign()
    override fun getAntiExfilCorruptionForTxSign() = qaTester.getAntiExfilCorruptionForTxSign()
    override fun getFirmwareCorruption() = qaTester.getFirmwareCorruption()

    fun setJadeFirmwareManager(jadeFirmwareManager: JadeFirmwareManager) {
        hardwareConnect.setJadeFirmwareManager(jadeFirmwareManager)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            applicationContext: Context,
            device: Device
        ): DeviceInfoViewModel
    }

    companion object : KLogging() {
        const val REQUIRE_REBONDING = "REQUIRE_REBONDING"

        fun provideFactory(
            assistedFactory: AssistedFactory,
            applicationContext: Context,
            device: Device
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(applicationContext, device) as T
            }
        }
    }
}