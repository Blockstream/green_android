package com.blockstream.green.ui.devices

import android.content.Context
import androidx.lifecycle.*
import com.blockstream.JadeHWWallet
import com.blockstream.gdk.GdkBridge
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
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import mu.KLogging

class DeviceInfoViewModel @AssistedInject constructor(
    @ApplicationContext val context: Context,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    deviceManager: DeviceManager,
    qaTester: QATester,
    val gdkBridge: GdkBridge,
    val settingsManager: SettingsManager,
    countly: Countly,
    @Assisted override val device: Device,
) : AbstractDeviceViewModel(sessionManager, walletRepository, deviceManager, qaTester, countly, null), HardwareConnectInteraction {

    // Don't use onProgress for this screen as is not turned off for animation reasons
    val navigationLock = MutableLiveData(false)

    val rememberDevice = MutableLiveData(true)

    val jadeIsUninitialized = MutableLiveData(false)

    val deviceState = device.deviceState.asLiveData()

    override val deviceConnectionManagerOrNull = DeviceConnectionManager(
        makeDeviceReady = false,
        countly = countly,
        gdkBridge = gdkBridge,
        settingsManager = settingsManager,
        httpRequestProvider = sessionManager.httpRequestProvider,
        interaction = this,
        qaTester = qaTester
    )

    init {
        if(device.hwWallet == null){
            unlockDevice(context)
        }else{
            // TODO get the operating network
        }
    }

    fun upgradeFirmware(jadeFirmwareManager: JadeFirmwareManager) {
        authenticateAndContinue(jadeFirmwareManager = jadeFirmwareManager)
    }

    private fun unlockDevice(context: Context) {
        onProgress.value = true
        navigationLock.value = true
        deviceConnectionManager.connectDevice(context, device)
        countly.hardwareConnect(device)
    }

    fun authenticateAndContinue(jadeFirmwareManager: JadeFirmwareManager? = null){
        val hwWallet = device.hwWallet ?: return

        doUserAction({
            // Authenticate device if needed
            deviceConnectionManager.authenticateDeviceIfNeeded(hwWallet = hwWallet, jadeFirmwareManager = jadeFirmwareManager)

            val network = deviceConnectionManager.getOperatingNetwork(hwWallet)
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

            if (device.isJade && jadeIsUninitialized.value == true && (device.hwWallet as? JadeHWWallet)?.isReady == true) {
                countly.jadeInitialize()
            }

            var session = sessionManager.getOnBoardingSession().also {
                // Disconnect any previous hww connection
                it.disconnect()
            }

            val walletHashId = session.getWalletIdentifier(
                network = network, // xPub generation is network agnostic
                hwWallet = device.hwWallet,
                hwWalletBridge = this
            ).walletHashId

            val wallet: Wallet
            if (isEphemeral) {
                wallet = Wallet.createEphemeralWallet(
                    ephemeralId = 0,
                    networkId = network.id,
                    name = device.name,
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
                    name = device.name,
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