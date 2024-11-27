package com.blockstream.common.models.devices

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_setup_guide
import blockstream_green.common.generated.resources.id_your_device_was_disconnected
import com.blockstream.common.data.DeviceIdentifier
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.devices.DeviceState
import com.blockstream.common.devices.JadeDevice
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.getWallet
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewGreenDevice
import com.blockstream.common.gdk.params.RsaVerifyParams
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.randomChars
import com.blockstream.jade.firmware.JadeFirmwareManager
import com.juul.kable.ConnectionLostException
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

abstract class DeviceInfoViewModelAbstract : AbstractDeviceViewModel() {
    override fun screenName(): String = "DeviceInfo"

    @NativeCoroutinesState
    abstract val jadeIsUninitialized: StateFlow<Boolean>
}

@OptIn(ExperimentalStdlibApi::class)
class DeviceInfoViewModel constructor(deviceId: String) : DeviceInfoViewModelAbstract() {

    private val _jadeIsUninitialized = MutableStateFlow(false)
    override val jadeIsUninitialized: StateFlow<Boolean> = _jadeIsUninitialized

    val deviceIsConnected = MutableStateFlow(false)

    class LocalEvents {
        data class AuthenticateAndContinue(val updateFirmwareFromChannel: String? = null) : Event
        data class SelectEnviroment(val isTestnet: Boolean?): Event

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

        if(deviceOrNull == null){
            postSideEffect(SideEffects.NavigateBack())
        }else {

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
                onBackPressed = {
                    !onProgress.value
                },
                actions = listOfNotNull(
                    NavAction(title = getString(Res.string.id_setup_guide), onClick = {
                        postEvent(NavigateDestinations.JadeGuide)
                    }).takeIf { deviceOrNull?.isJade == true },
                    NavAction(title = "Update Firmware", isMenuEntry = true, onClick = {
                        postSideEffect(LocalSideEffects.SelectFirmwareChannel())
                    }).takeIf { appInfo.isDevelopmentOrDebug && deviceOrNull?.isJade == true },
                    NavAction(title = "Genuine Check", isMenuEntry = true, onClick = {
                        genuineCheck()
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
        } else if(event is LocalEvents.SelectEnviroment) {

            if (event.isTestnet == null) {
                requestNetworkEmitter?.completeExceptionally(Exception("id_action_canceled"))
            } else {
                requestNetworkEmitter?.also {
                     it.complete(if(event.isTestnet) gdk.networks().testnetBitcoinElectrum else gdk.networks().bitcoinElectrum)
                }
            }
        }
    }

    private fun genuineCheck(){
        doAsync({
            (device as JadeDevice).let {

                val challenge = randomChars(32).encodeToByteArray()
                val attestation = it.jadeApi!!.signAttestation(challenge = challenge)

                val verifyChallenge = sessionManager.httpRequestHandler.rsaVerify(
                    RsaVerifyParams(
                        pem = attestation.pubkeyPem,
                        challenge = challenge.toHexString(),
                        signature = attestation.signature.toHexString()
                    )
                )
                val verifyPubKey = sessionManager.httpRequestHandler.rsaVerify(
                    RsaVerifyParams(
                        pem = RsaVerifyParams.VerifyingAuthorityPubKey,
                        challenge = attestation.pubkeyPem.encodeToByteArray().toHexString(),
                        signature = attestation.extSignature.toHexString()
                    )
                )

                if (verifyChallenge.result == true && verifyPubKey.result == true){
                    true
                } else {
                    throw Exception(verifyChallenge.error ?: verifyPubKey.error)
                }
            }

        }, onSuccess = {
            postSideEffect(SideEffects.Dialog(title = StringHolder.create("Success"), message = StringHolder.create("Your device is Genuine")))
        })
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
        }, onError = {
            it.printStackTrace()

            if (it is ConnectionLostException) {
                connectDevice()
            } else {
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
                                brand = device.deviceBrand,
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

            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Login(greenWallet = it, deviceId = device.connectionIdentifier)))
        })
    }

    companion object: Loggable()
}

class DeviceInfoViewModelPreview : DeviceInfoViewModelAbstract() {
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