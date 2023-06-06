package com.blockstream.green.ui.devices

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.*
import com.blockstream.common.gdk.Gdk
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.DeviceIdentifier
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.devices.HardwareConnectInteraction
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.logException
import com.blockstream.green.gdk.getWallet
import com.blockstream.green.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import mu.KLogging

class DeviceInfoViewModel @AssistedInject constructor(
    @SuppressLint("StaticFieldLeak")
    @ApplicationContext val context: Context,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    deviceManager: DeviceManager,
    qaTester: QATester,
    val gdk: Gdk,
    val settingsManager: SettingsManager,
    countly: Countly,
    @Assisted override val device: Device,
) : AbstractDeviceViewModel(sessionManager, walletRepository, deviceManager, qaTester, countly, null), HardwareConnectInteraction {

    // Don't use onProgress for this screen as is not turned off for animation reasons
    val navigationLock = MutableLiveData(false)

    val rememberDevice = MutableLiveData(false)

    val jadeIsUninitialized = MutableLiveData(false)

    val deviceIsConnected = MutableLiveData(false)

    val deviceState = device.deviceState.asLiveData()

    override val deviceConnectionManagerOrNull = DeviceConnectionManager(
        countly = countly,
        gdk = gdk,
        settingsManager = settingsManager,
        httpRequestProvider = sessionManager.httpRequestProvider,
        interaction = this,
        qaTester = qaTester
    )

    init {
        viewModelScope.launch(context = logException(countly)) {
            rememberDevice.postValue(settingsManager.rememberDeviceWallet())
        }

        if(device.gdkHardwareWallet == null){
            unlockDevice(context)
        }
    }

    private fun unlockDevice(context: Context) {
        onProgress.value = true
        navigationLock.value = true
        deviceConnectionManager.connectDevice(context, device)
        countly.hardwareConnect(device)
    }

    fun authenticateAndContinue(jadeFirmwareManager: JadeFirmwareManager? = null){
        val gdkHardwareWallet = device.gdkHardwareWallet ?: return

        doUserAction({
            // Save user preference
            settingsManager.setRememberDeviceWallet(rememberDeviceWallet = rememberDevice.value == true)

            // Authenticate device if needed
            deviceConnectionManager.authenticateDeviceIfNeeded(gdkHardwareWallet = gdkHardwareWallet, jadeFirmwareManager = jadeFirmwareManager)

            val network = deviceConnectionManager.getOperatingNetwork(gdkHardwareWallet)
            val isEphemeral = !rememberDevice.boolean()

            var previousSession = (if(device.isLedger){
                sessionManager.getDeviceSessionForNetworkAllPolicies(device, network, isEphemeral)
            }else{
                sessionManager.getDeviceSessionForEnvironment(device, network.isTestnet, isEphemeral)
            })

            if(previousSession != null){
                // Session already setup
                previousSession.getWallet(walletRepository)?.also {
                    return@doUserAction it
                }
            }

            var session = sessionManager.getOnBoardingSession().also {
                // Disconnect any previous hww connection
                it.disconnect()
            }

            val walletHashId = getWalletHashId(session, network, device)
            // Disable Jade wallet fingerprint, keep the device name // getWalletName(session, network, device)
            val walletName = device.name

            val wallet: Wallet
            if (isEphemeral) {
                wallet = Wallet.createEphemeralWallet(
                    networkId = network.id,
                    name = walletName,
                    isHardware = true,
                    isTestnet = network.isTestnet
                ).also {
                    session.ephemeralWallet = it
                }

                sessionManager.upgradeOnBoardingSessionToWallet(wallet)
            } else {
                // Persist wallet and device identifier
                wallet = walletRepository.getWalletWithHashId(
                    walletHashId = walletHashId,
                    isTestnet = network.isTestnet,
                    isHardware = true
                ) ?: Wallet(
                    walletHashId = walletHashId,
                    name = walletName,
                    activeNetwork = network.id,
                    activeAccount = 0,
                    isRecoveryPhraseConfirmed = true,
                    isHardware = true,
                    isTestnet = network.isTestnet
                )

                wallet.deviceIdentifiers = ((wallet.deviceIdentifiers ?: listOf()) + listOf(
                    DeviceIdentifier(
                        name = device.name,
                        uniqueIdentifier = device.uniqueIdentifier,
                        brand = device.deviceBrand,
                        connectionType = device.type
                    )
                )).toSet().toList() // Make it unique

                // Get ID from Room
                wallet.id = walletRepository.insertOrReplaceWallet(wallet)

                session = sessionManager.getWalletSession(wallet)

                countly.importWallet(session)
            }
            
            wallet
        }, postAction = {
            navigationLock.value = false
            onProgress.value = it == null
        }, onSuccess = { wallet: Wallet ->
            proceedToLogin = true
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(wallet)))
        })
    }

    override fun onDeviceReady(device: Device, isJadeUninitialized: Boolean?) {
        onProgress.postValue(false)
        navigationLock.postValue(false)
        deviceIsConnected.postValue(true)
        countly.hardwareConnected(device)
        jadeIsUninitialized.postValue(isJadeUninitialized == true)
    }

    override fun onDeviceFailed(device: Device) {
        super.onDeviceFailed(device)
        onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateBack()))
        navigationLock.postValue(false)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            device: Device
        ): DeviceInfoViewModel
    }

    companion object : KLogging() {
        const val REQUIRE_REBONDING = "REQUIRE_REBONDING"

        fun provideFactory(
            assistedFactory: AssistedFactory,
            device: Device
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(device) as T
            }
        }
    }
}