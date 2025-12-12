package com.blockstream.compose.models.devices

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_setup_guide
import blockstream_green.common.generated.resources.id_your_device_was_disconnected
import com.blockstream.common.data.DeviceIdentifier
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.DeviceState
import com.blockstream.common.devices.jadeDevice
import com.blockstream.common.extensions.getWallet
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewGreenDevice
import com.blockstream.common.gdk.events.JadeGenuineCheck
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.events.Event
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable
import com.blockstream.jade.firmware.JadeFirmwareManager
import com.juul.kable.NotConnectedException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class DeviceInfoViewModelAbstract(val deviceId: String) : AbstractDeviceViewModel() {
    override fun screenName(): String = "DeviceInfo"

    abstract val jadeIsUninitialized: StateFlow<Boolean>
}

class DeviceInfoViewModel(deviceId: String) : DeviceInfoViewModelAbstract(deviceId = deviceId) {

    private val _jadeIsUninitialized = MutableStateFlow(false)
    override val jadeIsUninitialized: StateFlow<Boolean> = _jadeIsUninitialized

    private val deviceIsConnected = MutableStateFlow(false)

    class LocalEvents {
        data class AuthenticateAndContinue(val updateFirmwareFromChannel: String? = null) : Event
        data class SelectEnviroment(val isTestnet: Boolean?) : Event
        data object GenuineCheckSuccess : Event
    }

    class LocalSideEffects {
        data class SelectFirmwareChannel(
            val channels: List<String> = listOf(
                JadeFirmwareManager.JADE_FW_VERSIONS_BETA,
                JadeFirmwareManager.JADE_FW_VERSIONS_LATEST,
                JadeFirmwareManager.JADE_FW_VERSIONS_PREVIOUS
            )
        ) : SideEffect
    }

    init {
        deviceOrNull = deviceManager.getDevice(deviceId)

        if (deviceOrNull == null) {
            postSideEffect(SideEffects.NavigateBack())
        } else {

            if (device.gdkHardwareWallet == null) {
                connectDevice()
            }

            device.deviceState.onEach {
                // Device went offline
                if (it == DeviceState.DISCONNECTED) {
                    postSideEffect(SideEffects.Snackbar(StringHolder(stringResource = Res.string.id_your_device_was_disconnected)))
                    postSideEffect(SideEffects.NavigateBack())
                }
            }.launchIn(this)
        }

        viewModelScope.launch {
            _navData.value = NavData(
                title = deviceOrNull?.deviceBrand?.name,
                isVisible = !onProgress.value,
                actions = listOfNotNull(
                    NavAction(title = getString(Res.string.id_setup_guide), onClick = {
                        postEvent(NavigateDestinations.JadeGuide)
                    }).takeIf { deviceOrNull?.isJade == true },
                    NavAction(title = "Update Firmware", isMenuEntry = true, onClick = {
                        postSideEffect(LocalSideEffects.SelectFirmwareChannel())
                    }).takeIf { appInfo.isDevelopmentOrDebug && deviceOrNull?.isJade == true },
                    NavAction(title = "Genuine Check", isMenuEntry = true, onClick = {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.JadeGenuineCheck(
                                    greenWalletOrNull = greenWalletOrNull,
                                    deviceId = deviceId
                                )
                            )
                        )
                    }).takeIf { appInfo.isDevelopmentOrDebug && deviceOrNull?.isJade == true }

                )
            )
        }

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.AuthenticateAndContinue) {
            authenticateAndContinue(event.updateFirmwareFromChannel)
        } else if (event is LocalEvents.SelectEnviroment) {

            if (event.isTestnet == null) {
                requestNetworkEmitter?.completeExceptionally(Exception("id_action_canceled"))
            } else {
                requestNetworkEmitter?.also {
                    it.complete(if (event.isTestnet) gdk.networks().testnetBitcoinElectrum else gdk.networks().bitcoinElectrum)
                }
            }
        }
    }

    private fun connectDevice() {
        doAsync({
            deviceConnectionManager.connectDevice(device, sessionManager.httpRequestHandler, this).also {
                countly.hardwareConnect(device)
            }
        }, onSuccess = {
            deviceIsConnected.value = true
            countly.hardwareConnected(device)
            _jadeIsUninitialized.value = it.isJadeUninitialized == true

            device.jadeDevice()?.also { jadeDevice ->

                val noEvent = jadeDevice.jadeApi?.getVersionInfo()?.efuseMac?.let { efuseMac ->
                    database.eventExist(JadeGenuineCheck(jadeId = efuseMac).sha256())
                } == false

                if (jadeDevice.supportsGenuineCheck() && noEvent) {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.NewJadeConnected))
                }
            }

        }, onError = {
            it.printStackTrace()

            if (it is NotConnectedException) {
                connectDevice()
            } else {
                postSideEffect(SideEffects.ErrorSnackbar(it))
                postSideEffect(SideEffects.NavigateBack())
            }
        })
    }

    private fun authenticateAndContinue(updateFirmwareFromChannel: String? = null) {
        val gdkHardwareWallet = device.gdkHardwareWallet ?: return

        doAsync({
            // Authenticate device if needed
            deviceConnectionManager.authenticateDeviceIfNeeded(
                gdkHardwareWallet = gdkHardwareWallet,
                interaction = this,
                httpRequestHandler = sessionManager.httpRequestHandler,
                jadeFirmwareManager = updateFirmwareFromChannel?.let {
                    JadeFirmwareManager(
                        firmwareInteraction = this,
                        httpRequestHandler = sessionManager.httpRequestHandler,
                        jadeFwVersionsFile = it,
                        forceFirmwareUpdate = true
                    )
                }
            )

            val network = device.getOperatingNetwork(device, gdk, interaction = this)!!
            val isEphemeral = !settingsManager.appSettings.rememberHardwareDevices

            val previousSession = (if (device.isLedger) {
                sessionManager.getDeviceSessionForNetworkAllPolicies(device, network, isEphemeral)
            } else {
                sessionManager.getDeviceSessionForEnvironment(
                    device,
                    network.isTestnet,
                    isEphemeral
                )
            })

            if (previousSession != null) {
                // Session already setup
                previousSession.getWallet(database, sessionManager)?.also {
                    return@doAsync it
                }
            }

            var session = sessionManager.getOnBoardingSession().also {
                // Disconnect any previous hww connection
                it.disconnect()
            }

            val walletHashId = getWalletHashId(session, network, device)
            // Disable Jade wallet fingerprint, keep the device name // getWalletName(session, network, device)
            val walletName = device.name

            val wallet: GreenWallet
            if (isEphemeral) {
                wallet = GreenWallet.createEphemeralWallet(
                    networkId = network.id,
                    name = walletName,
                    isHardware = true,
                    isTestnet = network.isTestnet
                ).also {
                    session.setEphemeralWallet(it)
                }

                sessionManager.upgradeOnBoardingSessionToWallet(wallet)
            } else {

                var isNewWallet = false

                // Persist wallet and device identifier
                wallet = database.getWalletWithXpubHashId(
                    xPubHashId = walletHashId,
                    isTestnet = network.isTestnet,
                    isHardware = true
                )?.let {
                    if (device.isLedger) {
                        // Change network based on user applet
                        it.activeNetwork = network.id
                    }
                    it
                } ?: run {
                    isNewWallet = true
                    GreenWallet.createWallet(
                        xPubHashId = walletHashId,
                        name = walletName,
                        activeNetwork = network.id,
                        activeAccount = 0,
                        isHardware = true,
                        isTestnet = network.isTestnet,
                    )
                }

                val combinedLoginCredentials = (wallet.wallet.device_identifiers ?: listOf()) +
                        listOf(
                            DeviceIdentifier(
                                name = device.name,
                                uniqueIdentifier = device.uniqueIdentifier,
                                model = device.deviceModel,
                                connectionType = device.type
                            )
                        ).toSet().toList() // Make it unique

                wallet.deviceIdentifiers = combinedLoginCredentials

                if (isNewWallet) {
                    database.insertWallet(wallet)
                } else {
                    database.updateWallet(wallet)
                }

                session = sessionManager.getWalletSessionOrCreate(wallet)

                countly.importWallet(session)
            }

            wallet
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            disconnectDeviceOnCleared = false

            deviceManager.savedDevice = device

            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.Login(
                        greenWallet = it,
                        autoLoginWallet = true,
                        deviceId = device.connectionIdentifier
                    )
                )
            )
        })
    }

    companion object : Loggable()
}

class DeviceInfoViewModelPreview : DeviceInfoViewModelAbstract(deviceId = "") {
    override val jadeIsUninitialized: StateFlow<Boolean> = MutableStateFlow(false)

    init {
        deviceOrNull = previewGreenDevice()

        onProgress.value = true

        viewModelScope.launch {
            delay(3_000)
            onProgress.value = false
        }
    }

    companion object {
        fun preview() =
            DeviceInfoViewModelPreview()
    }

}