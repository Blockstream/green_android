package com.blockstream.common.gdk.device

import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Device
import com.blockstream.common.gdk.data.InputOutput
import com.blockstream.common.gdk.data.Network
import com.blockstream.jade.firmware.FirmwareInteraction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow

interface HardwareConnectInteraction : FirmwareInteraction, HwWalletLogin, HardwareWalletInteraction {
    fun showInstructions(text: String)
    fun showError(err: String)

    fun requestPinBlocking(deviceBrand: DeviceBrand): String
}

interface HwWalletLogin: HardwareWalletInteraction {
    fun requestNetwork(): Network?
}

interface HardwareWalletInteraction{
    fun interactionRequest(gdkHardwareWallet: GdkHardwareWallet, message: String?, isMasterBlindingKeyRequest: Boolean, completable: CompletableDeferred<Boolean>?)
    fun requestPinMatrix(deviceBrand: DeviceBrand?): String?
    fun requestPassphrase(deviceBrand: DeviceBrand?): String?
}

abstract class GdkHardwareWallet {
    abstract val disconnectEvent: StateFlow<Boolean>?

    abstract val firmwareVersion: String?

    abstract val model: String

    abstract val device: Device

    // Return the base58check encoded xpubs for each path in paths
    abstract fun getXpubs(
        network: Network,
        paths: List<List<Int>>,
        hwInteraction: HardwareWalletInteraction?
    ): List<String>

    // Sign message with the key resulting from path, and return it as hex encoded DER
    // If using Anti-Exfil protocol, also return the signerCommitment (if not this can be null).
    abstract fun signMessage(
        path: List<Int>,
        message: String,
        useAeProtocol: Boolean,
        aeHostCommitment: String?,
        aeHostEntropy: String?,
        hwInteraction: HardwareWalletInteraction?
    ): SignMessageResult

    abstract fun signTransaction(
        network: Network,
        transaction: String,
        inputs: List<InputOutput>,
        outputs: List<InputOutput>,
        transactions: Map<String, String>?,
        useAeProtocol: Boolean,
        hwInteraction: HardwareWalletInteraction?
    ): SignTransactionResult

    abstract fun getBlindingFactors(
        inputs: List<InputOutput>,
        outputs: List<InputOutput>,
        hwInteraction: HardwareWalletInteraction?
    ): BlindingFactorsResult

    abstract fun getMasterBlindingKey(
        hwInteraction: HardwareWalletInteraction?,
    ): String

    abstract fun getBlindingNonce(
        pubKey: String,
        scriptHex: String,
        hwInteraction: HardwareWalletInteraction?
    ): String

    abstract fun getBlindingKey(
        scriptHex: String,
        hwInteraction: HardwareWalletInteraction?
    ): String

    @Throws(Exception::class)
    abstract fun getGreenAddress(
        network: Network,
        account: Account,
        path: List<Long>,
        csvBlocks: Long,
        hwInteraction: HardwareWalletInteraction?
    ): String

    abstract fun disconnect()
}